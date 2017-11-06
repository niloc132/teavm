/*
 *  Copyright 2016 Google Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.teavm.classlib.java.util.stream;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
import java.util.function.UnaryOperator;
import org.teavm.classlib.java.util.TArrays;
import org.teavm.classlib.java.util.TCollections;
import org.teavm.classlib.java.util.TComparator;
import org.teavm.classlib.java.util.TSpliterator;
import org.teavm.classlib.java.util.TSpliterators;
import org.teavm.classlib.java.util.TSpliterators.AbstractSpliterator;

/**
 * See <a href="https://docs.oracle.com/javase/8/docs/api/java/util/stream/Stream.html">
 * the official Java API doc</a> for details.
 *
 * @param <T> the type of data being streamed
 */
public interface TStream<T> extends TBaseStream<T, TStream<T>> {

    /**
     * See <a href="https://docs.oracle.com/javase/8/docs/api/java/util/stream/Stream.Builder.html">
     * the official Java API doc</a> for details.
     */
    interface Builder<T> extends Consumer<T> {
        @Override
        void accept(T t);

        default TStream.Builder<T> add(T t) {
            accept(t);
            return this;
        }

        TStream<T> build();
    }

    static <T> TStream.Builder<T> builder() {
        return new Builder<T>() {
            private Object[] items = new Object[0];

            @Override
            public void accept(T t) {
                if (items == null) {
                    throw new IllegalStateException("Builder already built");
                }
                items[items.length] = t;
            }

            @Override
            @SuppressWarnings("unchecked")
            public TStream<T> build() {
                if (items == null) {
                    throw new IllegalStateException("Builder already built");
                }
                TStream<T> stream = (TStream<T>) Arrays.stream(items);
                items = null;
                return stream;
            }
        };
    }

    static <T> TStream<T> concat(TStream<? extends T> a, TStream<? extends T> b) {
        // This is nearly the same as flatMap, but inlined, wrapped around a single spliterator of
        // these two objects, and without close() called as the stream progresses. Instead, close is
        // invoked as part of the resulting stream's own onClose, so that either can fail without
        // affecting the other, and correctly collecting suppressed exceptions.

        // TODO replace this flatMap-ish spliterator with one that directly combines the two root
        // streams
        TSpliterator<? extends TStream<? extends T>> spliteratorOfStreams =
                TArrays.asList(a, b).spliterator();

        AbstractSpliterator<T> spliterator =
                new TSpliterators.AbstractSpliterator<T>(Long.MAX_VALUE, 0) {
                    TSpliterator<? extends T> next;

                    @Override
                    public boolean tryAdvance(Consumer<? super T> action) {
                        // look for a new spliterator
                        while (advanceToNextSpliterator()) {
                            // if we have one, try to read and use it
                            if (next.tryAdvance(action)) {
                                return true;
                            } else {
                                // failed, null it out so we can find another
                                next = null;
                            }
                        }
                        return false;
                    }

                    private boolean advanceToNextSpliterator() {
                        while (next == null) {
                            if (!spliteratorOfStreams.tryAdvance(
                                    n -> {
                                        if (n != null) {
                                            next = n.spliterator();
                                        }
                                    })) {
                                return false;
                            }
                        }
                        return true;
                    }
                };

        TStream<T> result = new StreamImpl<T>(null, spliterator);

        return result.onClose(a::close).onClose(b::close);
    }

    static <T> TStream<T> empty() {
        return new StreamImpl.Empty<T>(null);
    }

    static <T> TStream<T> generate(Supplier<T> s) {
        AbstractSpliterator<T> spliterator =
                new TSpliterators.AbstractSpliterator<T>(
                        Long.MAX_VALUE, TSpliterator.IMMUTABLE | TSpliterator.ORDERED) {
                    @Override
                    public boolean tryAdvance(Consumer<? super T> action) {
                        action.accept(s.get());
                        return true;
                    }
                };
        return TStreamSupport.stream(spliterator, false);
    }

    static <T> TStream<T> iterate(T seed, UnaryOperator<T> f) {
        AbstractSpliterator<T> spliterator =
                new TSpliterators.AbstractSpliterator<T>(
                        Long.MAX_VALUE, TSpliterator.IMMUTABLE | TSpliterator.ORDERED) {
                    private T next = seed;

                    @Override
                    public boolean tryAdvance(Consumer<? super T> action) {
                        action.accept(next);
                        next = f.apply(next);
                        return true;
                    }
                };
        return TStreamSupport.stream(spliterator, false);
    }

    static <T> TStream<T> of(T t) {
        // TODO consider a splittable that returns only a single value, either for use here or in the
        //      singleton collection types
        return TCollections.singleton(t).stream();
    }

    static <T> TStream<T> of(T... values) {
        return TArrays.stream(values);
    }

    boolean allMatch(Predicate<? super T> predicate);

    boolean anyMatch(Predicate<? super T> predicate);

    <R, A> R collect(TCollector<? super T, A, R> collector);

    <R> R collect(
            Supplier<R> supplier, BiConsumer<R, ? super T> accumulator, BiConsumer<R, R> combiner);

    long count();

    TStream<T> distinct();

    TStream<T> filter(Predicate<? super T> predicate);

    Optional<T> findAny();

    Optional<T> findFirst();

    <R> TStream<R> flatMap(Function<? super T, ? extends TStream<? extends R>> mapper);

    TDoubleStream flatMapToDouble(Function<? super T, ? extends TDoubleStream> mapper);

    TIntStream flatMapToInt(Function<? super T, ? extends TIntStream> mapper);

    TLongStream flatMapToLong(Function<? super T, ? extends TLongStream> mapper);

    void forEach(Consumer<? super T> action);

    void forEachOrdered(Consumer<? super T> action);

    TStream<T> limit(long maxSize);

    <R> TStream<R> map(Function<? super T, ? extends R> mapper);

    TDoubleStream mapToDouble(ToDoubleFunction<? super T> mapper);

    TIntStream mapToInt(ToIntFunction<? super T> mapper);

    TLongStream mapToLong(ToLongFunction<? super T> mapper);

    Optional<T> max(Comparator<? super T> comparator);

    Optional<T> min(Comparator<? super T> comparator);

    boolean noneMatch(Predicate<? super T> predicate);

    TStream<T> peek(Consumer<? super T> action);

    Optional<T> reduce(BinaryOperator<T> accumulator);

    T reduce(T identity, BinaryOperator<T> accumulator);

    <U> U reduce(U identity, BiFunction<U, ? super T, U> accumulator, BinaryOperator<U> combiner);

    TStream<T> skip(long n);

    TStream<T> sorted();

    TStream<T> sorted(TComparator<? super T> comparator);

    Object[] toArray();

    <A> A[] toArray(IntFunction<A[]> generator);
}

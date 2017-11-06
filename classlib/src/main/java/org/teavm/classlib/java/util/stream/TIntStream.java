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

import java.util.IntSummaryStatistics;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.function.BiConsumer;
import java.util.function.IntBinaryOperator;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import java.util.function.IntSupplier;
import java.util.function.IntToDoubleFunction;
import java.util.function.IntToLongFunction;
import java.util.function.IntUnaryOperator;
import java.util.function.ObjIntConsumer;
import java.util.function.Supplier;
import org.teavm.classlib.java.util.TArrays;
import org.teavm.classlib.java.util.TComparator;
import org.teavm.classlib.java.util.TPrimitiveIterator;
import org.teavm.classlib.java.util.TSpliterator;
import org.teavm.classlib.java.util.TSpliterators;
import org.teavm.classlib.java.util.TSpliterators.AbstractIntSpliterator;

/**
 * See <a href="https://docs.oracle.com/javase/8/docs/api/java/util/stream/IntStream.html">
 * the official Java API doc</a> for details.
 */
public interface TIntStream extends TBaseStream<Integer, TIntStream> {

    /**
     * See <a
     * href="https://docs.oracle.com/javase/8/docs/api/java/util/stream/IntStream.Builder.html">the
     * official Java API doc</a> for details.
     */
    interface Builder extends IntConsumer {
        @Override
        void accept(int t);

        default TIntStream.Builder add(int t) {
            accept(t);
            return this;
        }

        TIntStream build();
    }

    static Builder builder() {
        return new Builder() {
            private int[] items = new int[0];

            @Override
            public void accept(int t) {
                if (items == null) {
                    throw new IllegalStateException("Builder already built");
                }
                items[items.length] = t;
            }

            @Override
            public TIntStream build() {
                if (items == null) {
                    throw new IllegalStateException("Builder already built");
                }
                TIntStream stream = TArrays.stream(items);
                items = null;
                return stream;
            }
        };
    }

    static TIntStream concat(TIntStream a, TIntStream b) {
        // This is nearly the same as flatMap, but inlined, wrapped around a single spliterator of
        // these two objects, and without close() called as the stream progresses. Instead, close is
        // invoked as part of the resulting stream's own onClose, so that either can fail without
        // affecting the other, and correctly collecting suppressed exceptions.

        // TODO replace this flatMap-ish spliterator with one that directly combines the two root
        // streams
        TSpliterator<? extends TIntStream> spliteratorOfStreams = TArrays.asList(a, b).spliterator();

        TSpliterator.OfInt spliterator =
                new TSpliterators.AbstractIntSpliterator(Long.MAX_VALUE, 0) {
                    TSpliterator.OfInt next;

                    @Override
                    public boolean tryAdvance(IntConsumer action) {
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

        TIntStream result = new IntStreamImpl(null, spliterator);

        return result.onClose(a::close).onClose(b::close);
    }

    static TIntStream empty() {
        return new IntStreamImpl.Empty(null);
    }

    static TIntStream generate(final IntSupplier s) {
        AbstractIntSpliterator spliterator =
                new TSpliterators.AbstractIntSpliterator(
                        Long.MAX_VALUE, TSpliterator.IMMUTABLE | TSpliterator.ORDERED) {
                    @Override
                    public boolean tryAdvance(IntConsumer action) {
                        action.accept(s.getAsInt());
                        return true;
                    }
                };

        return TStreamSupport.intStream(spliterator, false);
    }

    static TIntStream iterate(int seed, IntUnaryOperator f) {

        AbstractIntSpliterator spliterator =
                new TSpliterators.AbstractIntSpliterator(
                        Long.MAX_VALUE, TSpliterator.IMMUTABLE | TSpliterator.ORDERED) {
                    private int next = seed;

                    @Override
                    public boolean tryAdvance(IntConsumer action) {
                        action.accept(next);
                        next = f.applyAsInt(next);
                        return true;
                    }
                };

        return TStreamSupport.intStream(spliterator, false);
    }

    static TIntStream of(int... values) {
        return TArrays.stream(values);
    }

    static TIntStream of(int t) {
        // TODO consider a splittable that returns only a single value
        return of(new int[] {t});
    }

    static TIntStream range(int startInclusive, int endExclusive) {
        if (startInclusive >= endExclusive) {
            return empty();
        }
        return rangeClosed(startInclusive, endExclusive - 1);
    }

    static TIntStream rangeClosed(int startInclusive, int endInclusive) {
        if (startInclusive > endInclusive) {
            return empty();
        }
        int count = endInclusive - startInclusive + 1;

        AbstractIntSpliterator spliterator =
                new TSpliterators.AbstractIntSpliterator(
                        count,
                        TSpliterator.IMMUTABLE
                                | TSpliterator.SIZED
                                | TSpliterator.SUBSIZED
                                | TSpliterator.ORDERED
                                | TSpliterator.SORTED
                                | TSpliterator.DISTINCT) {
                    private int next = startInclusive;

                    @Override
                    public TComparator<? super Integer> getComparator() {
                        return null;
                    }

                    @Override
                    public boolean tryAdvance(IntConsumer action) {
                        if (next <= endInclusive) {
                            action.accept(next++);
                            return true;
                        }
                        return false;
                    }
                };

        return TStreamSupport.intStream(spliterator, false);
    }

    boolean allMatch(IntPredicate predicate);

    boolean anyMatch(IntPredicate predicate);

    TDoubleStream asDoubleStream();

    TLongStream asLongStream();

    OptionalDouble average();

    TStream<Integer> boxed();

    <R> R collect(Supplier<R> supplier, ObjIntConsumer<R> accumulator, BiConsumer<R, R> combiner);

    long count();

    TIntStream distinct();

    TIntStream filter(IntPredicate predicate);

    OptionalInt findAny();

    OptionalInt findFirst();

    TIntStream flatMap(IntFunction<? extends TIntStream> mapper);

    void forEach(IntConsumer action);

    void forEachOrdered(IntConsumer action);

    @Override
    TPrimitiveIterator.OfInt iterator();

    TIntStream limit(long maxSize);

    TIntStream map(IntUnaryOperator mapper);

    TDoubleStream mapToDouble(IntToDoubleFunction mapper);

    TLongStream mapToLong(IntToLongFunction mapper);

    <U> TStream<U> mapToObj(IntFunction<? extends U> mapper);

    OptionalInt max();

    OptionalInt min();

    boolean noneMatch(IntPredicate predicate);

    @Override
    TIntStream parallel();

    TIntStream peek(IntConsumer action);

    OptionalInt reduce(IntBinaryOperator op);

    int reduce(int identity, IntBinaryOperator op);

    @Override
    TIntStream sequential();

    TIntStream skip(long n);

    TIntStream sorted();

    @Override
    TSpliterator.OfInt spliterator();

    int sum();

    IntSummaryStatistics summaryStatistics();

    int[] toArray();
}

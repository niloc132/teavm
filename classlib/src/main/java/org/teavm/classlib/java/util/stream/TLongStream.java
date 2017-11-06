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

import java.util.LongSummaryStatistics;
import java.util.OptionalDouble;
import java.util.OptionalLong;
import java.util.function.BiConsumer;
import java.util.function.LongBinaryOperator;
import java.util.function.LongConsumer;
import java.util.function.LongFunction;
import java.util.function.LongPredicate;
import java.util.function.LongSupplier;
import java.util.function.LongToDoubleFunction;
import java.util.function.LongToIntFunction;
import java.util.function.LongUnaryOperator;
import java.util.function.ObjLongConsumer;
import java.util.function.Supplier;
import org.teavm.classlib.java.util.TArrays;
import org.teavm.classlib.java.util.TComparator;
import org.teavm.classlib.java.util.TPrimitiveIterator;
import org.teavm.classlib.java.util.TSpliterator;
import org.teavm.classlib.java.util.TSpliterators;
import org.teavm.classlib.java.util.TSpliterators.AbstractLongSpliterator;

/**
 * See <a href="https://docs.oracle.com/javase/8/docs/api/java/util/stream/LongStream.html">
 * the official Java API doc</a> for details.
 */
public interface TLongStream extends TBaseStream<Long, TLongStream> {

    /**
     * See <a
     * href="https://docs.oracle.com/javase/8/docs/api/java/util/stream/LongStream.Builder.html">the
     * official Java API doc</a> for details.
     */
    interface Builder extends LongConsumer {
        @Override
        void accept(long t);

        default TLongStream.Builder add(long t) {
            accept(t);
            return this;
        }

        TLongStream build();
    }

    static Builder builder() {
        return new Builder() {
            private long[] items = new long[0];

            @Override
            public void accept(long t) {
                if (items == null) {
                    throw new IllegalStateException("Builder already built");
                }
                items[items.length] = t;
            }

            @Override
            public TLongStream build() {
                if (items == null) {
                    throw new IllegalStateException("Builder already built");
                }
                TLongStream stream = TArrays.stream(items);
                items = null;
                return stream;
            }
        };
    }

    static TLongStream concat(TLongStream a, TLongStream b) {
        // This is nearly the same as flatMap, but inlined, wrapped around a single spliterator of
        // these two objects, and without close() called as the stream progresses. Instead, close is
        // invoked as part of the resulting stream's own onClose, so that either can fail without
        // affecting the other, and correctly collecting suppressed exceptions.

        // TODO replace this flatMap-ish spliterator with one that directly combines the two root
        // streams
        TSpliterator<? extends TLongStream> spliteratorOfStreams = TArrays.asList(a, b).spliterator();

        AbstractLongSpliterator spliterator =
                new TSpliterators.AbstractLongSpliterator(Long.MAX_VALUE, 0) {
                    TSpliterator.OfLong next;

                    @Override
                    public boolean tryAdvance(LongConsumer action) {
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

        TLongStream result = new LongStreamImpl(null, spliterator);

        return result.onClose(a::close).onClose(b::close);
    }

    static TLongStream empty() {
        return new LongStreamImpl.Empty(null);
    }

    static TLongStream generate(LongSupplier s) {
        AbstractLongSpliterator spltierator =
                new TSpliterators.AbstractLongSpliterator(
                        Long.MAX_VALUE, TSpliterator.IMMUTABLE | TSpliterator.ORDERED) {
                    @Override
                    public boolean tryAdvance(LongConsumer action) {
                        action.accept(s.getAsLong());
                        return true;
                    }
                };

        return TStreamSupport.longStream(spltierator, false);
    }

    static TLongStream iterate(long seed, LongUnaryOperator f) {
        AbstractLongSpliterator spliterator =
                new TSpliterators.AbstractLongSpliterator(
                        Long.MAX_VALUE, TSpliterator.IMMUTABLE | TSpliterator.ORDERED) {
                    private long next = seed;

                    @Override
                    public boolean tryAdvance(LongConsumer action) {
                        action.accept(next);
                        next = f.applyAsLong(next);
                        return true;
                    }
                };
        return TStreamSupport.longStream(spliterator, false);
    }

    static TLongStream of(long... values) {
        return TArrays.stream(values);
    }

    static TLongStream of(long t) {
        // TODO consider a splittable that returns only a single value
        return of(new long[] {t});
    }

    static TLongStream range(long startInclusive, long endExclusive) {
        if (startInclusive >= endExclusive) {
            return empty();
        }
        return rangeClosed(startInclusive, endExclusive - 1);
    }

    static TLongStream rangeClosed(long startInclusive, long endInclusive) {
        if (startInclusive > endInclusive) {
            return empty();
        }
        long count = endInclusive - startInclusive + 1;

        AbstractLongSpliterator spliterator =
                new TSpliterators.AbstractLongSpliterator(
                        count,
                        TSpliterator.IMMUTABLE
                                | TSpliterator.SIZED
                                | TSpliterator.SUBSIZED
                                | TSpliterator.ORDERED
                                | TSpliterator.SORTED
                                | TSpliterator.DISTINCT) {
                    private long next = startInclusive;

                    @Override
                    public TComparator<? super Long> getComparator() {
                        return null;
                    }

                    @Override
                    public boolean tryAdvance(LongConsumer action) {
                        if (next <= endInclusive) {
                            action.accept(next++);
                            return true;
                        }
                        return false;
                    }
                };

        return TStreamSupport.longStream(spliterator, false);
    }

    boolean allMatch(LongPredicate predicate);

    boolean anyMatch(LongPredicate predicate);

    TDoubleStream asDoubleStream();

    OptionalDouble average();

    TStream<Long> boxed();

    <R> R collect(Supplier<R> supplier, ObjLongConsumer<R> accumulator, BiConsumer<R, R> combiner);

    long count();

    TLongStream distinct();

    TLongStream filter(LongPredicate predicate);

    OptionalLong findAny();

    OptionalLong findFirst();

    TLongStream flatMap(LongFunction<? extends TLongStream> mapper);

    void forEach(LongConsumer action);

    void forEachOrdered(LongConsumer action);

    @Override
    TPrimitiveIterator.OfLong iterator();

    TLongStream limit(long maxSize);

    TLongStream map(LongUnaryOperator mapper);

    TDoubleStream mapToDouble(LongToDoubleFunction mapper);

    TIntStream mapToInt(LongToIntFunction mapper);

    <U> TStream<U> mapToObj(LongFunction<? extends U> mapper);

    OptionalLong max();

    OptionalLong min();

    boolean noneMatch(LongPredicate predicate);

    @Override
    TLongStream parallel();

    TLongStream peek(LongConsumer action);

    OptionalLong reduce(LongBinaryOperator op);

    long reduce(long identity, LongBinaryOperator op);

    @Override
    TLongStream sequential();

    TLongStream skip(long n);

    TLongStream sorted();

    @Override
    TSpliterator.OfLong spliterator();

    long sum();

    LongSummaryStatistics summaryStatistics();

    long[] toArray();
}

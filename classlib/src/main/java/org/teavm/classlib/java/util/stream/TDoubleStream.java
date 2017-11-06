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

import java.util.DoubleSummaryStatistics;
import java.util.OptionalDouble;
import java.util.function.BiConsumer;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.DoublePredicate;
import java.util.function.DoubleSupplier;
import java.util.function.DoubleToIntFunction;
import java.util.function.DoubleToLongFunction;
import java.util.function.DoubleUnaryOperator;
import java.util.function.ObjDoubleConsumer;
import java.util.function.Supplier;
import org.teavm.classlib.java.util.TArrays;
import org.teavm.classlib.java.util.TPrimitiveIterator;
import org.teavm.classlib.java.util.TSpliterator;
import org.teavm.classlib.java.util.TSpliterators;

/**
 * See <a href="https://docs.oracle.com/javase/8/docs/api/java/util/stream/DoubleStream.html">
 * the official Java API doc</a> for details.
 */
public interface TDoubleStream extends TBaseStream<Double, TDoubleStream> {

    /**
     * See <a
     * href="https://docs.oracle.com/javase/8/docs/api/java/util/stream/DoubleStream.Builder.html">the
     * official Java API doc</a> for details.
     */
    interface Builder extends DoubleConsumer {
        @Override
        void accept(double t);

        default TDoubleStream.Builder add(double t) {
            accept(t);
            return this;
        }

        TDoubleStream build();
    }

    static Builder builder() {
        return new Builder() {
            private double[] items = new double[0];

            @Override
            public void accept(double t) {
                if (items == null) {
                    throw new IllegalStateException("Builder already built");
                }
                items[items.length] = t;
            }

            @Override
            public TDoubleStream build() {
                if (items == null) {
                    throw new IllegalStateException("Builder already built");
                }
                TDoubleStream stream = TArrays.stream(items);
                items = null;
                return stream;
            }
        };
    }

    static TDoubleStream concat(TDoubleStream a, TDoubleStream b) {
        // This is nearly the same as flatMap, but inlined, wrapped around a single spliterator of
        // these two objects, and without close() called as the stream progresses. Instead, close is
        // invoked as part of the resulting stream's own onClose, so that either can fail without
        // affecting the other, and correctly collecting suppressed exceptions.

        // TODO replace this flatMap-ish spliterator with one that directly combines the two root
        // streams
        TSpliterator<? extends TDoubleStream> spliteratorOfStreams = TArrays.asList(a, b).spliterator();

        TSpliterator.OfDouble spliterator =
                new TSpliterators.AbstractDoubleSpliterator(Long.MAX_VALUE, 0) {
                    TSpliterator.OfDouble next;

                    @Override
                    public boolean tryAdvance(DoubleConsumer action) {
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

        TDoubleStream result = new DoubleStreamImpl(null, spliterator);

        return result.onClose(a::close).onClose(b::close);
    }

    static TDoubleStream empty() {
        return new DoubleStreamImpl.Empty(null);
    }

    static TDoubleStream generate(DoubleSupplier s) {
        TSpliterator.OfDouble spliterator =
                new TSpliterators.AbstractDoubleSpliterator(
                        Long.MAX_VALUE, TSpliterator.IMMUTABLE | TSpliterator.ORDERED) {
                    @Override
                    public boolean tryAdvance(DoubleConsumer action) {
                        action.accept(s.getAsDouble());
                        return true;
                    }
                };

        return TStreamSupport.doubleStream(spliterator, false);
    }

    static TDoubleStream iterate(double seed, DoubleUnaryOperator f) {
        TSpliterator.OfDouble spliterator =
                new TSpliterators.AbstractDoubleSpliterator(
                        Long.MAX_VALUE, TSpliterator.IMMUTABLE | TSpliterator.ORDERED) {
                    private double next = seed;

                    @Override
                    public boolean tryAdvance(DoubleConsumer action) {
                        action.accept(next);
                        next = f.applyAsDouble(next);
                        return true;
                    }
                };

        return TStreamSupport.doubleStream(spliterator, false);
    }

    static TDoubleStream of(double... values) {
        return TArrays.stream(values);
    }

    static TDoubleStream of(double t) {
        // TODO consider a splittable that returns only a single value
        return of(new double[] {t});
    }

    boolean allMatch(DoublePredicate predicate);

    boolean anyMatch(DoublePredicate predicate);

    OptionalDouble average();

    TStream<Double> boxed();

    <R> R collect(Supplier<R> supplier, ObjDoubleConsumer<R> accumulator, BiConsumer<R, R> combiner);

    long count();

    TDoubleStream distinct();

    TDoubleStream filter(DoublePredicate predicate);

    OptionalDouble findAny();

    OptionalDouble findFirst();

    TDoubleStream flatMap(DoubleFunction<? extends TDoubleStream> mapper);

    void forEach(DoubleConsumer action);

    void forEachOrdered(DoubleConsumer action);

    @Override
    TPrimitiveIterator.OfDouble iterator();

    TDoubleStream limit(long maxSize);

    TDoubleStream map(DoubleUnaryOperator mapper);

    TIntStream mapToInt(DoubleToIntFunction mapper);

    TLongStream mapToLong(DoubleToLongFunction mapper);

    <U> TStream<U> mapToObj(DoubleFunction<? extends U> mapper);

    OptionalDouble max();

    OptionalDouble min();

    boolean noneMatch(DoublePredicate predicate);

    @Override
    TDoubleStream parallel();

    TDoubleStream peek(DoubleConsumer action);

    OptionalDouble reduce(DoubleBinaryOperator op);

    double reduce(double identity, DoubleBinaryOperator op);

    @Override
    TDoubleStream sequential();

    TDoubleStream skip(long n);

    TDoubleStream sorted();

    @Override
    TSpliterator.OfDouble spliterator();

    double sum();

    DoubleSummaryStatistics summaryStatistics();

    double[] toArray();
}

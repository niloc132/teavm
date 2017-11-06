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
import java.util.HashSet;
import java.util.IntSummaryStatistics;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntBinaryOperator;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import java.util.function.IntToDoubleFunction;
import java.util.function.IntToLongFunction;
import java.util.function.IntUnaryOperator;
import java.util.function.LongConsumer;
import java.util.function.ObjIntConsumer;
import java.util.function.Supplier;
import org.teavm.classlib.java.util.TComparator;
import org.teavm.classlib.java.util.TPrimitiveIterator;
import org.teavm.classlib.java.util.TSpliterator;
import org.teavm.classlib.java.util.TSpliterators;
import org.teavm.classlib.java.util.TSpliterators.AbstractIntSpliterator;

/**
 * Main implementation of IntStream, wrapping a single spliterator, and an optional parent stream.
 */
final class IntStreamImpl extends TerminatableStream<IntStreamImpl> implements TIntStream {

    /**
     * Represents an empty stream, doing nothing for all methods.
     */
    static class Empty extends TerminatableStream<Empty> implements TIntStream {
        public Empty(TerminatableStream<?> previous) {
            super(previous);
        }

        @Override
        public TIntStream filter(IntPredicate predicate) {
            throwIfTerminated();
            return this;
        }

        @Override
        public TIntStream map(IntUnaryOperator mapper) {
            throwIfTerminated();
            return this;
        }

        @Override
        public <U> TStream<U> mapToObj(IntFunction<? extends U> mapper) {
            throwIfTerminated();
            return new StreamImpl.Empty<U>(this);
        }

        @Override
        public TLongStream mapToLong(IntToLongFunction mapper) {
            throwIfTerminated();
            return new LongStreamImpl.Empty(this);
        }

        @Override
        public TDoubleStream mapToDouble(IntToDoubleFunction mapper) {
            throwIfTerminated();
            return new DoubleStreamImpl.Empty(this);
        }

        @Override
        public TIntStream flatMap(IntFunction<? extends TIntStream> mapper) {
            throwIfTerminated();
            return this;
        }

        @Override
        public TIntStream distinct() {
            throwIfTerminated();
            return this;
        }

        @Override
        public TIntStream sorted() {
            throwIfTerminated();
            return this;
        }

        @Override
        public TIntStream peek(IntConsumer action) {
            throwIfTerminated();
            return this;
        }

        @Override
        public TIntStream limit(long maxSize) {
            throwIfTerminated();
            if (maxSize < 0) {
                throw new IllegalStateException("maxSize may not be negative");
            }
            return this;
        }

        @Override
        public TIntStream skip(long n) {
            throwIfTerminated();
            if (n < 0) {
                throw new IllegalStateException("n may not be negative");
            }
            return this;
        }

        @Override
        public void forEach(IntConsumer action) {
            terminate();
            // do nothing
        }

        @Override
        public void forEachOrdered(IntConsumer action) {
            terminate();
            // do nothing
        }

        @Override
        public int[] toArray() {
            terminate();
            return new int[0];
        }

        @Override
        public int reduce(int identity, IntBinaryOperator op) {
            terminate();
            return identity;
        }

        @Override
        public OptionalInt reduce(IntBinaryOperator op) {
            terminate();
            return OptionalInt.empty();
        }

        @Override
        public <R> R collect(
                Supplier<R> supplier, ObjIntConsumer<R> accumulator, BiConsumer<R, R> combiner) {
            terminate();
            return supplier.get();
        }

        @Override
        public int sum() {
            terminate();
            return 0;
        }

        @Override
        public OptionalInt min() {
            terminate();
            return OptionalInt.empty();
        }

        @Override
        public OptionalInt max() {
            terminate();
            return OptionalInt.empty();
        }

        @Override
        public long count() {
            terminate();
            return 0;
        }

        @Override
        public OptionalDouble average() {
            terminate();
            return OptionalDouble.empty();
        }

        @Override
        public IntSummaryStatistics summaryStatistics() {
            terminate();
            return new IntSummaryStatistics();
        }

        @Override
        public boolean anyMatch(IntPredicate predicate) {
            terminate();
            return false;
        }

        @Override
        public boolean allMatch(IntPredicate predicate) {
            terminate();
            return true;
        }

        @Override
        public boolean noneMatch(IntPredicate predicate) {
            terminate();
            return true;
        }

        @Override
        public OptionalInt findFirst() {
            terminate();
            return OptionalInt.empty();
        }

        @Override
        public OptionalInt findAny() {
            terminate();
            return OptionalInt.empty();
        }

        @Override
        public TLongStream asLongStream() {
            throwIfTerminated();
            return new LongStreamImpl.Empty(this);
        }

        @Override
        public TDoubleStream asDoubleStream() {
            throwIfTerminated();
            return new DoubleStreamImpl.Empty(this);
        }

        @Override
        public TStream<Integer> boxed() {
            throwIfTerminated();
            return new StreamImpl.Empty<>(this);
        }

        @Override
        public TIntStream sequential() {
            throwIfTerminated();
            return this;
        }

        @Override
        public TIntStream parallel() {
            throwIfTerminated();
            return this;
        }

        @Override
        public TPrimitiveIterator.OfInt iterator() {
            return TSpliterators.iterator(spliterator());
        }

        @Override
        public TSpliterator.OfInt spliterator() {
            terminate();
            return TSpliterators.emptyIntSpliterator();
        }

        @Override
        public boolean isParallel() {
            throwIfTerminated();
            return false;
        }

        @Override
        public TIntStream unordered() {
            throwIfTerminated();
            return this;
        }
    }

    /**
     * Int to Int map spliterator.
     */
    private static final class MapToIntSpliterator extends TSpliterators.AbstractIntSpliterator {
        private final IntUnaryOperator map;
        private final TSpliterator.OfInt original;

        public MapToIntSpliterator(IntUnaryOperator map, TSpliterator.OfInt original) {
            super(
                    original.estimateSize(),
                    original.characteristics() & ~(TSpliterator.SORTED | TSpliterator.DISTINCT));
            Objects.requireNonNull(map);
            this.map = map;
            this.original = original;
        }

        @Override
        public boolean tryAdvance(final IntConsumer action) {
            return original.tryAdvance((int u) -> action.accept(map.applyAsInt(u)));
        }
    }

    /**
     * Int to Object map spliterator.
     *
     * @param <T> the type of data in the object spliterator
     */
    private static final class MapToObjSpliterator<T> extends TSpliterators.AbstractSpliterator<T> {
        private final IntFunction<? extends T> map;
        private final TSpliterator.OfInt original;

        public MapToObjSpliterator(IntFunction<? extends T> map, TSpliterator.OfInt original) {
            super(
                    original.estimateSize(),
                    original.characteristics() & ~(TSpliterator.SORTED | TSpliterator.DISTINCT));
            Objects.requireNonNull(map);
            this.map = map;
            this.original = original;
        }

        @Override
        public boolean tryAdvance(final Consumer<? super T> action) {
            return original.tryAdvance((int u) -> action.accept(map.apply(u)));
        }
    }

    /**
     * Int to Long map spliterator.
     */
    private static final class MapToLongSpliterator extends TSpliterators.AbstractLongSpliterator {
        private final IntToLongFunction map;
        private final TSpliterator.OfInt original;

        public MapToLongSpliterator(IntToLongFunction map, TSpliterator.OfInt original) {
            super(
                    original.estimateSize(),
                    original.characteristics() & ~(TSpliterator.SORTED | TSpliterator.DISTINCT));
            Objects.requireNonNull(map);
            this.map = map;
            this.original = original;
        }

        @Override
        public boolean tryAdvance(final LongConsumer action) {
            return original.tryAdvance((int u) -> action.accept(map.applyAsLong(u)));
        }
    }

    /**
     * Int to Double map spliterator.
     */
    private static final class MapToDoubleSpliterator extends TSpliterators.AbstractDoubleSpliterator {
        private final IntToDoubleFunction map;
        private final TSpliterator.OfInt original;

        public MapToDoubleSpliterator(IntToDoubleFunction map, TSpliterator.OfInt original) {
            super(
                    original.estimateSize(),
                    original.characteristics() & ~(TSpliterator.SORTED | TSpliterator.DISTINCT));
            Objects.requireNonNull(map);
            this.map = map;
            this.original = original;
        }

        @Override
        public boolean tryAdvance(final DoubleConsumer action) {
            return original.tryAdvance((int u) -> action.accept(map.applyAsDouble(u)));
        }
    }

    /**
     * Int filter spliterator.
     */
    private static final class FilterSpliterator extends TSpliterators.AbstractIntSpliterator {
        private final IntPredicate filter;
        private final TSpliterator.OfInt original;

        private boolean found;

        public FilterSpliterator(IntPredicate filter, TSpliterator.OfInt original) {
            super(
                    original.estimateSize(),
                    original.characteristics() & ~(TSpliterator.SIZED | TSpliterator.SUBSIZED));
            Objects.requireNonNull(filter);
            this.filter = filter;
            this.original = original;
        }

        @Override
        public TComparator<? super Integer> getComparator() {
            return original.getComparator();
        }

        @Override
        public boolean tryAdvance(final IntConsumer action) {
            found = false;
            while (!found
                    && original.tryAdvance(
                    (int item) -> {
                        if (filter.test(item)) {
                            found = true;
                            action.accept(item);
                        }
                    })) {
                // do nothing, work is done in tryAdvance
            }

            return found;
        }
    }

    /**
     * Int skip spliterator.
     */
    private static final class SkipSpliterator extends TSpliterators.AbstractIntSpliterator {
        private long skip;
        private final TSpliterator.OfInt original;

        public SkipSpliterator(long skip, TSpliterator.OfInt original) {
            super(
                    original.hasCharacteristics(TSpliterator.SIZED)
                            ? Math.max(0, original.estimateSize() - skip)
                            : Long.MAX_VALUE,
                    original.characteristics());
            this.skip = skip;
            this.original = original;
        }

        @Override
        public TComparator<? super Integer> getComparator() {
            return original.getComparator();
        }

        @Override
        public boolean tryAdvance(IntConsumer action) {
            while (skip > 0) {
                if (!original.tryAdvance((int ignore) -> { })) {
                    return false;
                }
                skip--;
            }
            return original.tryAdvance(action);
        }
    }

    /**
     * Int limit spliterator.
     */
    private static final class LimitSpliterator extends TSpliterators.AbstractIntSpliterator {
        private final long limit;
        private final TSpliterator.OfInt original;
        private int position;

        public LimitSpliterator(long limit, TSpliterator.OfInt original) {
            super(
                    original.hasCharacteristics(TSpliterator.SIZED)
                            ? Math.min(original.estimateSize(), limit)
                            : Long.MAX_VALUE,
                    original.characteristics());
            this.limit = limit;
            this.original = original;
        }

        @Override
        public TComparator<? super Integer> getComparator() {
            return original.getComparator();
        }

        @Override
        public boolean tryAdvance(IntConsumer action) {
            if (position >= limit) {
                return false;
            }
            boolean result = original.tryAdvance(action);
            position++;
            return result;
        }
    }

    /**
     * Value holder for various stream operations.
     */
    private static final class ValueConsumer implements IntConsumer {
        int value;

        @Override
        public void accept(int value) {
            this.value = value;
        }
    }

    private final TSpliterator.OfInt spliterator;

    public IntStreamImpl(TerminatableStream<?> previous, TSpliterator.OfInt spliterator) {
        super(previous);
        this.spliterator = spliterator;
    }

    // terminals
    @Override
    public TSpliterator.OfInt spliterator() {
        terminate();
        return spliterator;
    }

    @Override
    public TPrimitiveIterator.OfInt iterator() {
        return TSpliterators.iterator(spliterator());
    }

    @Override
    public OptionalInt findFirst() {
        terminate();
        ValueConsumer holder = new ValueConsumer();
        if (spliterator.tryAdvance(holder)) {
            return OptionalInt.of(holder.value);
        }
        return OptionalInt.empty();
    }

    @Override
    public OptionalInt findAny() {
        return findFirst();
    }

    @Override
    public boolean noneMatch(IntPredicate predicate) {
        return !anyMatch(predicate);
    }

    @Override
    public boolean allMatch(IntPredicate predicate) {
        return !anyMatch(predicate.negate());
    }

    @Override
    public boolean anyMatch(IntPredicate predicate) {
        return filter(predicate).findFirst().isPresent();
    }

    @Override
    public IntSummaryStatistics summaryStatistics() {
        return collect(
                IntSummaryStatistics::new,
                // TODO switch to a lambda reference once #9340 is fixed
                (intSummaryStatistics, value) -> intSummaryStatistics.accept(value),
                IntSummaryStatistics::combine);
    }

    @Override
    public OptionalDouble average() {
        IntSummaryStatistics stats = summaryStatistics();
        if (stats.getCount() == 0) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.of(stats.getAverage());
    }

    @Override
    public long count() {
        terminate();
        long count = 0;
        while (spliterator.tryAdvance((int value) -> { })) {
            count++;
        }
        return count;
    }

    @Override
    public OptionalInt max() {
        IntSummaryStatistics stats = summaryStatistics();
        if (stats.getCount() == 0) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(stats.getMax());
    }

    @Override
    public OptionalInt min() {
        IntSummaryStatistics stats = summaryStatistics();
        if (stats.getCount() == 0) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(stats.getMin());
    }

    @Override
    public int sum() {
        return (int) summaryStatistics().getSum();
    }

    @Override
    public <R> R collect(
            Supplier<R> supplier, final ObjIntConsumer<R> accumulator, BiConsumer<R, R> combiner) {
        terminate();
        final R acc = supplier.get();
        spliterator.forEachRemaining((int value) -> accumulator.accept(acc, value));
        return acc;
    }

    @Override
    public OptionalInt reduce(IntBinaryOperator op) {
        ValueConsumer holder = new ValueConsumer();
        if (spliterator.tryAdvance(holder)) {
            return OptionalInt.of(reduce(holder.value, op));
        }
        terminate();
        return OptionalInt.empty();
    }

    @Override
    public int reduce(int identity, IntBinaryOperator op) {
        terminate();
        ValueConsumer holder = new ValueConsumer();
        holder.value = identity;
        spliterator.forEachRemaining(
                (int value) -> {
                    holder.accept(op.applyAsInt(holder.value, value));
                });
        return holder.value;
    }

    @Override
    public int[] toArray() {
        terminate();
        int[] entries = new int[0];
        // this is legal in js, since the array will be backed by a JS array
        spliterator.forEachRemaining((int value) -> entries[entries.length] = value);

        return entries;
    }

    @Override
    public void forEachOrdered(IntConsumer action) {
        terminate();
        spliterator.forEachRemaining(action);
    }

    @Override
    public void forEach(IntConsumer action) {
        forEachOrdered(action);
    }
    // end terminals

    // intermediates

    @Override
    public TIntStream filter(IntPredicate predicate) {
        throwIfTerminated();
        return new IntStreamImpl(this, new FilterSpliterator(predicate, spliterator));
    }

    @Override
    public TIntStream map(IntUnaryOperator mapper) {
        throwIfTerminated();
        return new IntStreamImpl(this, new MapToIntSpliterator(mapper, spliterator));
    }

    @Override
    public <U> TStream<U> mapToObj(IntFunction<? extends U> mapper) {
        throwIfTerminated();
        return new StreamImpl<U>(this, new MapToObjSpliterator<U>(mapper, spliterator));
    }

    @Override
    public TLongStream mapToLong(IntToLongFunction mapper) {
        throwIfTerminated();
        return new LongStreamImpl(this, new MapToLongSpliterator(mapper, spliterator));
    }

    @Override
    public TDoubleStream mapToDouble(IntToDoubleFunction mapper) {
        throwIfTerminated();
        return new DoubleStreamImpl(this, new MapToDoubleSpliterator(mapper, spliterator));
    }

    @Override
    public TIntStream flatMap(IntFunction<? extends TIntStream> mapper) {
        throwIfTerminated();
        final TSpliterator<? extends TIntStream> spliteratorOfStreams =
                new MapToObjSpliterator<>(mapper, spliterator);

        TSpliterator.OfInt flatMapSpliterator =
                new TSpliterators.AbstractIntSpliterator(Long.MAX_VALUE, 0) {
                    TIntStream nextStream;
                    TSpliterator.OfInt next;

                    @Override
                    public boolean tryAdvance(IntConsumer action) {
                        // look for a new spliterator
                        while (advanceToNextSpliterator()) {
                            // if we have one, try to read and use it
                            if (next.tryAdvance(action)) {
                                return true;
                            } else {
                                nextStream.close();
                                nextStream = null;
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
                                            nextStream = n;
                                            next = n.spliterator();
                                        }
                                    })) {
                                return false;
                            }
                        }
                        return true;
                    }
                };

        return new IntStreamImpl(this, flatMapSpliterator);
    }

    @Override
    public TIntStream distinct() {
        throwIfTerminated();
        HashSet<Integer> seen = new HashSet<>();
        return filter(seen::add);
    }

    @Override
    public TIntStream sorted() {
        throwIfTerminated();

        AbstractIntSpliterator sortedSpliterator =
                new TSpliterators.AbstractIntSpliterator(
                        spliterator.estimateSize(), spliterator.characteristics() | TSpliterator.SORTED) {
                    TSpliterator.OfInt ordered;

                    @Override
                    public TComparator<? super Integer> getComparator() {
                        return null;
                    }

                    @Override
                    public boolean tryAdvance(IntConsumer action) {
                        if (ordered == null) {
                            int[] list = new int[0];
                            spliterator.forEachRemaining((int item) -> list[list.length] = item);
                            Arrays.sort(list);
                            ordered = TSpliterators.spliterator(list, characteristics());
                        }
                        return ordered.tryAdvance(action);
                    }
                };

        return new IntStreamImpl(this, sortedSpliterator);
    }

    @Override
    public TIntStream peek(IntConsumer action) {
        Objects.requireNonNull(action);
        throwIfTerminated();

        AbstractIntSpliterator peekSpliterator =
                new TSpliterators.AbstractIntSpliterator(
                        spliterator.estimateSize(), spliterator.characteristics()) {
                    @Override
                    public boolean tryAdvance(final IntConsumer innerAction) {
                        return spliterator.tryAdvance(action.andThen(innerAction));
                    }
                };

        return new IntStreamImpl(this, peekSpliterator);
    }

    @Override
    public TIntStream limit(long maxSize) {
        throwIfTerminated();
        if (maxSize < 0) {
            throw new IllegalStateException("maxSize may not be negative");
        }
        return new IntStreamImpl(this, new LimitSpliterator(maxSize, spliterator));
    }

    @Override
    public TIntStream skip(long n) {
        throwIfTerminated();
        if (n < 0) {
            throw new IllegalStateException("n may not be negative");
        }
        if (n == 0) {
            return this;
        }
        return new IntStreamImpl(this, new SkipSpliterator(n, spliterator));
    }

    @Override
    public TLongStream asLongStream() {
        return mapToLong(i -> (long) i);
    }

    @Override
    public TDoubleStream asDoubleStream() {
        return mapToDouble(i -> (double) i);
    }

    @Override
    public TStream<Integer> boxed() {
        return mapToObj(Integer::valueOf);
    }

    @Override
    public TIntStream sequential() {
        throwIfTerminated();
        return this;
    }

    @Override
    public TIntStream parallel() {
        throwIfTerminated();
        return this;
    }

    @Override
    public boolean isParallel() {
        throwIfTerminated();
        return false;
    }

    @Override
    public TIntStream unordered() {
        throwIfTerminated();
        return this;
    }
}

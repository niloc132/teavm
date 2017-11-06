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
import java.util.LongSummaryStatistics;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.OptionalLong;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.LongBinaryOperator;
import java.util.function.LongConsumer;
import java.util.function.LongFunction;
import java.util.function.LongPredicate;
import java.util.function.LongToDoubleFunction;
import java.util.function.LongToIntFunction;
import java.util.function.LongUnaryOperator;
import java.util.function.ObjLongConsumer;
import java.util.function.Supplier;
import org.teavm.classlib.java.util.TComparator;
import org.teavm.classlib.java.util.TPrimitiveIterator;
import org.teavm.classlib.java.util.TSpliterator;
import org.teavm.classlib.java.util.TSpliterators;
import org.teavm.classlib.java.util.TSpliterators.AbstractLongSpliterator;

/**
 * Main implementation of LongStream, wrapping a single spliterator, and an optional parent stream.
 */
final class LongStreamImpl extends TerminatableStream<LongStreamImpl> implements TLongStream {

    /**
     * Represents an empty stream, doing nothing for all methods.
     */
    static class Empty extends TerminatableStream<Empty> implements TLongStream {
        public Empty(TerminatableStream<?> previous) {
            super(previous);
        }

        @Override
        public TLongStream filter(LongPredicate predicate) {
            throwIfTerminated();
            return this;
        }

        @Override
        public TLongStream map(LongUnaryOperator mapper) {
            throwIfTerminated();
            return this;
        }

        @Override
        public <U> TStream<U> mapToObj(LongFunction<? extends U> mapper) {
            throwIfTerminated();
            return new StreamImpl.Empty<U>(this);
        }

        @Override
        public TIntStream mapToInt(LongToIntFunction mapper) {
            throwIfTerminated();
            return new IntStreamImpl.Empty(this);
        }

        @Override
        public TDoubleStream mapToDouble(LongToDoubleFunction mapper) {
            throwIfTerminated();
            return new DoubleStreamImpl.Empty(this);
        }

        @Override
        public TLongStream flatMap(LongFunction<? extends TLongStream> mapper) {
            throwIfTerminated();
            return this;
        }

        @Override
        public TLongStream distinct() {
            throwIfTerminated();
            return this;
        }

        @Override
        public TLongStream sorted() {
            throwIfTerminated();
            return this;
        }

        @Override
        public TLongStream peek(LongConsumer action) {
            throwIfTerminated();
            return this;
        }

        @Override
        public TLongStream limit(long maxSize) {
            throwIfTerminated();
            if (maxSize < 0) {
                throw new IllegalStateException("maxSize may not be negative");
            }
            return this;
        }

        @Override
        public TLongStream skip(long n) {
            throwIfTerminated();
            if (n >= 0) {
                throw new IllegalStateException("n may not be negative");
            }
            return this;
        }

        @Override
        public void forEach(LongConsumer action) {
            terminate();
        }

        @Override
        public void forEachOrdered(LongConsumer action) {
            terminate();
        }

        @Override
        public long[] toArray() {
            terminate();
            return new long[0];
        }

        @Override
        public long reduce(long identity, LongBinaryOperator op) {
            terminate();
            return identity;
        }

        @Override
        public OptionalLong reduce(LongBinaryOperator op) {
            terminate();
            return OptionalLong.empty();
        }

        @Override
        public <R> R collect(
                Supplier<R> supplier, ObjLongConsumer<R> accumulator, BiConsumer<R, R> combiner) {
            terminate();
            return supplier.get();
        }

        @Override
        public long sum() {
            terminate();
            return 0;
        }

        @Override
        public OptionalLong min() {
            terminate();
            return OptionalLong.empty();
        }

        @Override
        public OptionalLong max() {
            terminate();
            return OptionalLong.empty();
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
        public LongSummaryStatistics summaryStatistics() {
            terminate();
            return new LongSummaryStatistics();
        }

        @Override
        public boolean anyMatch(LongPredicate predicate) {
            terminate();
            return false;
        }

        @Override
        public boolean allMatch(LongPredicate predicate) {
            terminate();
            return true;
        }

        @Override
        public boolean noneMatch(LongPredicate predicate) {
            terminate();
            return true;
        }

        @Override
        public OptionalLong findFirst() {
            terminate();
            return OptionalLong.empty();
        }

        @Override
        public OptionalLong findAny() {
            terminate();
            return OptionalLong.empty();
        }

        @Override
        public TDoubleStream asDoubleStream() {
            throwIfTerminated();
            return new DoubleStreamImpl.Empty(this);
        }

        @Override
        public TStream<Long> boxed() {
            throwIfTerminated();
            return new StreamImpl.Empty<Long>(this);
        }

        @Override
        public TLongStream sequential() {
            throwIfTerminated();
            return this;
        }

        @Override
        public TLongStream parallel() {
            throwIfTerminated();
            return this;
        }

        @Override
        public TPrimitiveIterator.OfLong iterator() {
            return TSpliterators.iterator(spliterator());
        }

        @Override
        public TSpliterator.OfLong spliterator() {
            terminate();
            return TSpliterators.emptyLongSpliterator();
        }

        @Override
        public boolean isParallel() {
            throwIfTerminated();
            return false;
        }

        @Override
        public TLongStream unordered() {
            throwIfTerminated();
            return this;
        }
    }

    /**
     * Long to Int map spliterator.
     */
    private static final class MapToIntSpliterator extends TSpliterators.AbstractIntSpliterator {
        private final LongToIntFunction map;
        private final TSpliterator.OfLong original;

        public MapToIntSpliterator(LongToIntFunction map, TSpliterator.OfLong original) {
            super(
                    original.estimateSize(),
                    original.characteristics() & ~(TSpliterator.SORTED | TSpliterator.DISTINCT));
            Objects.requireNonNull(map);
            this.map = map;
            this.original = original;
        }

        @Override
        public boolean tryAdvance(final IntConsumer action) {
            return original.tryAdvance((long u) -> action.accept(map.applyAsInt(u)));
        }
    }

    /**
     * Long to Object map spliterator.
     *
     * @param <T> the type of data in the object spliterator
     */
    private static final class MapToObjSpliterator<T> extends TSpliterators.AbstractSpliterator<T> {
        private final LongFunction<? extends T> map;
        private final TSpliterator.OfLong original;

        public MapToObjSpliterator(LongFunction<? extends T> map, TSpliterator.OfLong original) {
            super(
                    original.estimateSize(),
                    original.characteristics() & ~(TSpliterator.SORTED | TSpliterator.DISTINCT));
            Objects.requireNonNull(map);
            this.map = map;
            this.original = original;
        }

        @Override
        public boolean tryAdvance(final Consumer<? super T> action) {
            return original.tryAdvance((long u) -> action.accept(map.apply(u)));
        }
    }

    /**
     * Long to Long map spliterator.
     */
    private static final class MapToLongSpliterator extends TSpliterators.AbstractLongSpliterator {
        private final LongUnaryOperator map;
        private final TSpliterator.OfLong original;

        public MapToLongSpliterator(LongUnaryOperator map, TSpliterator.OfLong original) {
            super(
                    original.estimateSize(),
                    original.characteristics() & ~(TSpliterator.SORTED | TSpliterator.DISTINCT));
            Objects.requireNonNull(map);
            this.map = map;
            this.original = original;
        }

        @Override
        public boolean tryAdvance(final LongConsumer action) {
            return original.tryAdvance((long u) -> action.accept(map.applyAsLong(u)));
        }
    }

    /**
     * Long to Double map TSpliterator.
     */
    private static final class MapToDoubleSpliterator extends TSpliterators.AbstractDoubleSpliterator {
        private final LongToDoubleFunction map;
        private final TSpliterator.OfLong original;

        public MapToDoubleSpliterator(LongToDoubleFunction map, TSpliterator.OfLong original) {
            super(
                    original.estimateSize(),
                    original.characteristics() & ~(TSpliterator.SORTED | TSpliterator.DISTINCT));
            Objects.requireNonNull(map);
            this.map = map;
            this.original = original;
        }

        @Override
        public boolean tryAdvance(final DoubleConsumer action) {
            return original.tryAdvance((long u) -> action.accept(map.applyAsDouble(u)));
        }
    }

    /**
     * Long filter spliterator.
     */
    private static final class FilterSpliterator extends TSpliterators.AbstractLongSpliterator {
        private final LongPredicate filter;
        private final TSpliterator.OfLong original;

        private boolean found;

        public FilterSpliterator(LongPredicate filter, TSpliterator.OfLong original) {
            super(
                    original.estimateSize(),
                    original.characteristics() & ~(TSpliterator.SIZED | TSpliterator.SUBSIZED));
            Objects.requireNonNull(filter);
            this.filter = filter;
            this.original = original;
        }

        @Override
        public TComparator<? super Long> getComparator() {
            return original.getComparator();
        }

        @Override
        public boolean tryAdvance(final LongConsumer action) {
            found = false;
            while (!found
                    && original.tryAdvance(
                    (long item) -> {
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
     * Long skip spliterator.
     */
    private static final class SkipSpliterator extends TSpliterators.AbstractLongSpliterator {
        private long skip;
        private final TSpliterator.OfLong original;

        public SkipSpliterator(long skip, TSpliterator.OfLong original) {
            super(
                    original.hasCharacteristics(TSpliterator.SIZED)
                            ? Math.max(0, original.estimateSize() - skip)
                            : Long.MAX_VALUE,
                    original.characteristics());
            this.skip = skip;
            this.original = original;
        }

        @Override
        public TComparator<? super Long> getComparator() {
            return original.getComparator();
        }

        @Override
        public boolean tryAdvance(LongConsumer action) {
            while (skip > 0) {
                if (!original.tryAdvance((long ignore) -> { })) {
                    return false;
                }
                skip--;
            }
            return original.tryAdvance(action);
        }
    }

    /**
     * Long limit spliterator.
     */
    private static final class LimitSpliterator extends TSpliterators.AbstractLongSpliterator {
        private final long limit;
        private final TSpliterator.OfLong original;
        private int position;

        public LimitSpliterator(long limit, TSpliterator.OfLong original) {
            super(
                    original.hasCharacteristics(TSpliterator.SIZED)
                            ? Math.min(original.estimateSize(), limit)
                            : Long.MAX_VALUE,
                    original.characteristics());
            this.limit = limit;
            this.original = original;
        }

        @Override
        public TComparator<? super Long> getComparator() {
            return original.getComparator();
        }

        @Override
        public boolean tryAdvance(LongConsumer action) {
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
    private static final class ValueConsumer implements LongConsumer {
        long value;

        @Override
        public void accept(long value) {
            this.value = value;
        }
    }

    private final TSpliterator.OfLong spliterator;

    public LongStreamImpl(TerminatableStream<?> previous, TSpliterator.OfLong spliterator) {
        super(previous);
        this.spliterator = spliterator;
    }

    // terminals

    @Override
    public void forEach(LongConsumer action) {
        forEachOrdered(action);
    }

    @Override
    public void forEachOrdered(LongConsumer action) {
        terminate();
        spliterator.forEachRemaining(action);
    }

    @Override
    public long[] toArray() {
        terminate();
        long[] entries = new long[0];
        // this is legal in js, since the array will be backed by a JS array
        spliterator.forEachRemaining((long value) -> entries[entries.length] = value);

        return entries;
    }

    @Override
    public long reduce(long identity, LongBinaryOperator op) {
        terminate();
        ValueConsumer holder = new ValueConsumer();
        holder.value = identity;
        spliterator.forEachRemaining(
                (long value) -> {
                    holder.accept(op.applyAsLong(holder.value, value));
                });
        return holder.value;
    }

    @Override
    public OptionalLong reduce(LongBinaryOperator op) {
        ValueConsumer holder = new ValueConsumer();
        if (spliterator.tryAdvance(holder)) {
            return OptionalLong.of(reduce(holder.value, op));
        }
        terminate();
        return OptionalLong.empty();
    }

    @Override
    public <R> R collect(
            Supplier<R> supplier, ObjLongConsumer<R> accumulator, BiConsumer<R, R> combiner) {
        terminate();
        final R acc = supplier.get();
        spliterator.forEachRemaining((long value) -> accumulator.accept(acc, value));
        return acc;
    }

    @Override
    public long sum() {
        return summaryStatistics().getSum();
    }

    @Override
    public OptionalLong min() {
        LongSummaryStatistics stats = summaryStatistics();
        if (stats.getCount() == 0) {
            return OptionalLong.empty();
        }
        return OptionalLong.of(stats.getMin());
    }

    @Override
    public OptionalLong max() {
        LongSummaryStatistics stats = summaryStatistics();
        if (stats.getCount() == 0) {
            return OptionalLong.empty();
        }
        return OptionalLong.of(stats.getMax());
    }

    @Override
    public long count() {
        terminate();
        long count = 0;
        while (spliterator.tryAdvance((long value) -> { })) {
            count++;
        }
        return count;
    }

    @Override
    public OptionalDouble average() {
        LongSummaryStatistics stats = summaryStatistics();
        if (stats.getCount() == 0) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.of(stats.getAverage());
    }

    @Override
    public LongSummaryStatistics summaryStatistics() {
        return collect(
                LongSummaryStatistics::new,
                // TODO switch to a lambda reference once #9340 is fixed
                (longSummaryStatistics, value) -> longSummaryStatistics.accept(value),
                LongSummaryStatistics::combine);
    }

    @Override
    public boolean anyMatch(LongPredicate predicate) {
        return filter(predicate).findFirst().isPresent();
    }

    @Override
    public boolean allMatch(LongPredicate predicate) {
        return !anyMatch(predicate.negate());
    }

    @Override
    public boolean noneMatch(LongPredicate predicate) {
        return !anyMatch(predicate);
    }

    @Override
    public OptionalLong findFirst() {
        terminate();
        ValueConsumer holder = new ValueConsumer();
        if (spliterator.tryAdvance(holder)) {
            return OptionalLong.of(holder.value);
        }
        return OptionalLong.empty();
    }

    @Override
    public OptionalLong findAny() {
        return findFirst();
    }

    @Override
    public TPrimitiveIterator.OfLong iterator() {
        return TSpliterators.iterator(spliterator());
    }

    @Override
    public TSpliterator.OfLong spliterator() {
        terminate();
        return spliterator;
    }
    // end terminals

    // intermediates
    @Override
    public TLongStream filter(LongPredicate predicate) {
        throwIfTerminated();
        return new LongStreamImpl(this, new FilterSpliterator(predicate, spliterator));
    }

    @Override
    public TLongStream map(LongUnaryOperator mapper) {
        throwIfTerminated();
        return new LongStreamImpl(this, new MapToLongSpliterator(mapper, spliterator));
    }

    @Override
    public <U> TStream<U> mapToObj(LongFunction<? extends U> mapper) {
        throwIfTerminated();
        return new StreamImpl<U>(this, new MapToObjSpliterator<U>(mapper, spliterator));
    }

    @Override
    public TIntStream mapToInt(LongToIntFunction mapper) {
        throwIfTerminated();
        return new IntStreamImpl(this, new MapToIntSpliterator(mapper, spliterator));
    }

    @Override
    public TDoubleStream mapToDouble(LongToDoubleFunction mapper) {
        throwIfTerminated();
        return new DoubleStreamImpl(this, new MapToDoubleSpliterator(mapper, spliterator));
    }

    @Override
    public TLongStream flatMap(LongFunction<? extends TLongStream> mapper) {
        throwIfTerminated();
        final TSpliterator<? extends TLongStream> spliteratorOfStreams =
                new MapToObjSpliterator<>(mapper, spliterator);

        AbstractLongSpliterator flatMapSpliterator =
                new TSpliterators.AbstractLongSpliterator(Long.MAX_VALUE, 0) {
                    TLongStream nextStream;
                    TSpliterator.OfLong next;

                    @Override
                    public boolean tryAdvance(LongConsumer action) {
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

        return new LongStreamImpl(this, flatMapSpliterator);
    }

    @Override
    public TLongStream distinct() {
        throwIfTerminated();
        HashSet<Long> seen = new HashSet<>();
        return filter(seen::add);
    }

    @Override
    public TLongStream sorted() {
        throwIfTerminated();

        AbstractLongSpliterator sortedSpliterator =
                new TSpliterators.AbstractLongSpliterator(
                        spliterator.estimateSize(), spliterator.characteristics() | TSpliterator.SORTED) {
                    TSpliterator.OfLong ordered;

                    @Override
                    public TComparator<? super Long> getComparator() {
                        return null;
                    }

                    @Override
                    public boolean tryAdvance(LongConsumer action) {
                        if (ordered == null) {
                            long[] list = new long[0];
                            spliterator.forEachRemaining((long item) -> list[list.length] = item);
                            Arrays.sort(list);
                            ordered = TSpliterators.spliterator(list, characteristics());
                        }
                        return ordered.tryAdvance(action);
                    }
                };

        return new LongStreamImpl(this, sortedSpliterator);
    }

    @Override
    public TLongStream peek(LongConsumer action) {
        Objects.requireNonNull(action);
        throwIfTerminated();

        AbstractLongSpliterator peekSpliterator =
                new TSpliterators.AbstractLongSpliterator(
                        spliterator.estimateSize(), spliterator.characteristics()) {
                    @Override
                    public boolean tryAdvance(final LongConsumer innerAction) {
                        return spliterator.tryAdvance(action.andThen(innerAction));
                    }
                };

        return new LongStreamImpl(this, peekSpliterator);
    }

    @Override
    public TLongStream limit(long maxSize) {
        throwIfTerminated();
        if (maxSize < 0) {
            throw new IllegalStateException("maxSize may not be negative");
        }
        return new LongStreamImpl(this, new LimitSpliterator(maxSize, spliterator));
    }

    @Override
    public TLongStream skip(long n) {
        throwIfTerminated();
        if (n < 0) {
            throw new IllegalStateException("n may not be negative");
        }
        if (n == 0) {
            return this;
        }
        return new LongStreamImpl(this, new SkipSpliterator(n, spliterator));
    }

    @Override
    public TDoubleStream asDoubleStream() {
        return mapToDouble(x -> (double) x);
    }

    @Override
    public TStream<Long> boxed() {
        return mapToObj(Long::valueOf);
    }

    @Override
    public TLongStream sequential() {
        throwIfTerminated();
        return this;
    }

    @Override
    public TLongStream parallel() {
        throwIfTerminated();
        return this;
    }

    @Override
    public boolean isParallel() {
        throwIfTerminated();
        return false;
    }

    @Override
    public TLongStream unordered() {
        throwIfTerminated();
        return this;
    }
}

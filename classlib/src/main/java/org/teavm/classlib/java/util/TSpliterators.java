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
package org.teavm.classlib.java.util;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;

/**
 * See <a href="https://docs.oracle.com/javase/8/docs/api/java/util/Spliterators.html">
 * the official Java API doc</a> for details.
 *
 * Since it's hard to implement parallel algorithms in the browser environment
 * and to keep code simple, implementation does not provide splitting.
 */
public final class TSpliterators {

    private abstract static class BaseSpliterator<T, S extends TSpliterator<T>>
            implements TSpliterator<T> {
        private final int characteristics;
        private long sizeEstimate;

        BaseSpliterator(long size, int characteristics) {
            this.sizeEstimate = size;
            this.characteristics = (characteristics & TSpliterator.SIZED) != 0
                    ? characteristics | TSpliterator.SUBSIZED : characteristics;
        }

        public int characteristics() {
            return characteristics;
        }

        public long estimateSize() {
            return sizeEstimate;
        }

        public S trySplit() {
            // see javadoc for java.util.Spliterator
            return null;
        }
    }

    /**
     * See <a href="https://docs.oracle.com/javase/8/docs/api/java/util/Spliterators.AbstractSpliterator.html">
     * the official Java API doc</a> for details.
     */
    public abstract static class AbstractSpliterator<T>
            extends BaseSpliterator<T, TSpliterator<T>> implements TSpliterator<T> {

        protected AbstractSpliterator(long size, int characteristics) {
            super(size, characteristics);
        }
    }

    /**
     * See <a href="https://docs.oracle.com/javase/8/docs/api/java/util/Spliterators.AbstractDoubleSpliterator.html">
     * the official Java API doc</a> for details.
     */
    public abstract static class AbstractDoubleSpliterator
            extends BaseSpliterator<Double, TSpliterator.OfDouble> implements TSpliterator.OfDouble {

        protected AbstractDoubleSpliterator(long size, int characteristics) {
            super(size, characteristics);
        }
    }

    /**
     * See <a href="https://docs.oracle.com/javase/8/docs/api/java/util/Spliterators.AbstractIntSpliterator.html">
     * the official Java API doc</a> for details.
     */
    public abstract static class AbstractIntSpliterator
            extends BaseSpliterator<Integer, TSpliterator.OfInt> implements TSpliterator.OfInt {

        protected AbstractIntSpliterator(long size, int characteristics) {
            super(size, characteristics);
        }
    }

    /**
     * See <a href="https://docs.oracle.com/javase/8/docs/api/java/util/Spliterators.AbstractLongSpliterator.html">
     * the official Java API doc</a> for details.
     */
    public abstract static class AbstractLongSpliterator
            extends BaseSpliterator<Long, TSpliterator.OfLong> implements TSpliterator.OfLong {

        protected AbstractLongSpliterator(long size, int characteristics) {
            super(size, characteristics);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> TSpliterator<T> emptySpliterator() {
        return (TSpliterator<T>) EmptySpliterator.OF_REF;
    }

    public static TSpliterator.OfDouble emptyDoubleSpliterator() {
        return EmptySpliterator.OF_DOUBLE;
    }

    public static TSpliterator.OfInt emptyIntSpliterator() {
        return EmptySpliterator.OF_INT;
    }

    public static TSpliterator.OfLong emptyLongSpliterator() {
        return EmptySpliterator.OF_LONG;
    }

    public static <T> TSpliterator<T> spliterator(Object[] array, int characteristics) {
        return new ArraySpliterator<>(array, characteristics);
    }

    public static <T> TSpliterator<T> spliterator(Object[] array, int fromIndex, int toIndex,
            int characteristics) {
        checkCriticalArrayBounds(fromIndex, toIndex, array.length);
        return new ArraySpliterator<>(array, fromIndex, toIndex, characteristics);
    }

    public static TSpliterator.OfInt spliterator(int[] array, int characteristics) {
        return new IntArraySpliterator(array, characteristics);
    }

    public static TSpliterator.OfInt spliterator(int[] array, int fromIndex, int toIndex,
            int characteristics) {
        checkCriticalArrayBounds(fromIndex, toIndex, array.length);
        return new IntArraySpliterator(array, fromIndex, toIndex, characteristics);
    }

    public static TSpliterator.OfLong spliterator(long[] array, int characteristics) {
        return new LongArraySpliterator(array, characteristics);
    }

    public static TSpliterator.OfLong spliterator(long[] array, int fromIndex, int toIndex,
            int characteristics) {
        checkCriticalArrayBounds(fromIndex, toIndex, array.length);
        return new LongArraySpliterator(array, fromIndex, toIndex, characteristics);
    }

    public static TSpliterator.OfDouble spliterator(double[] array, int characteristics) {
        return new DoubleArraySpliterator(array, characteristics);
    }

    public static TSpliterator.OfDouble spliterator(double[] array, int fromIndex, int toIndex,
            int characteristics) {
        checkCriticalArrayBounds(fromIndex, toIndex, array.length);
        return new DoubleArraySpliterator(array, fromIndex, toIndex, characteristics);
    }

    public static <T> TSpliterator<T> spliterator(TCollection<? extends T> c, int characteristics) {
        return new IteratorSpliterator<>(c, characteristics);
    }

    public static <T> TSpliterator<T> spliterator(TIterator<? extends T> it, long size,
            int characteristics) {
        return new IteratorSpliterator<>(it, size, characteristics);
    }

    public static <T> TSpliterator<T> spliteratorUnknownSize(TIterator<? extends T> it,
            int characteristics) {
        return new IteratorSpliterator<>(it, characteristics);
    }

    public static TSpliterator.OfInt spliterator(TPrimitiveIterator.OfInt it, long size,
            int characteristics) {
        return new IntIteratorSpliterator(it, size, characteristics);
    }

    public static TSpliterator.OfInt spliteratorUnknownSize(TPrimitiveIterator.OfInt it,
            int characteristics) {
        return new IntIteratorSpliterator(it, characteristics);
    }

    public static TSpliterator.OfLong spliterator(TPrimitiveIterator.OfLong it, long size,
            int characteristics) {
        return new LongIteratorSpliterator(it, size, characteristics);
    }

    public static TSpliterator.OfLong spliteratorUnknownSize(TPrimitiveIterator.OfLong it,
            int characteristics) {
        return new LongIteratorSpliterator(it, characteristics);
    }

    public static TSpliterator.OfDouble spliterator(TPrimitiveIterator.OfDouble it, long size,
            int characteristics) {
        return new DoubleIteratorSpliterator(it, size, characteristics);
    }

    public static TSpliterator.OfDouble spliteratorUnknownSize(TPrimitiveIterator.OfDouble it,
            int characteristics) {
        return new DoubleIteratorSpliterator(it, characteristics);
    }

    public static <T> TIterator<T> iterator(TSpliterator<? extends T> spliterator) {
        return new ConsumerIterator<>(spliterator);
    }

    public static TPrimitiveIterator.OfDouble iterator(TSpliterator.OfDouble spliterator) {
        return new DoubleConsumerIterator(spliterator);
    }

    public static TPrimitiveIterator.OfInt iterator(TSpliterator.OfInt spliterator) {
        return new IntConsumerIterator(spliterator);
    }

    public static TPrimitiveIterator.OfLong iterator(TSpliterator.OfLong spliterator) {
        return new LongConsumerIterator(spliterator);
    }

    private abstract static class EmptySpliterator<T, S extends TSpliterator<T>, C>
            implements TSpliterator<T> {

        static final TSpliterator<Object> OF_REF = new EmptySpliterator.OfRef<>();
        static final TSpliterator.OfDouble OF_DOUBLE = new EmptySpliterator.OfDouble();
        static final TSpliterator.OfInt OF_INT = new EmptySpliterator.OfInt();
        static final TSpliterator.OfLong OF_LONG = new EmptySpliterator.OfLong();

        public int characteristics() {
            return TSpliterator.SIZED | TSpliterator.SUBSIZED;
        }

        public long estimateSize() {
            return 0;
        }

        public void forEachRemaining(C consumer) {
            Objects.requireNonNull(consumer);
        }

        public boolean tryAdvance(C consumer) {
            Objects.requireNonNull(consumer);
            return false;
        }

        public S trySplit() {
            return null;
        }

        private static final class OfRef<T>
                extends EmptySpliterator<T, TSpliterator<T>, Consumer<? super T>>
                implements TSpliterator<T> {

            OfRef() { }
        }

        private static final class OfDouble
                extends EmptySpliterator<Double, TSpliterator.OfDouble, DoubleConsumer>
                implements TSpliterator.OfDouble {

            OfDouble() { }
        }

        private static final class OfInt
                extends EmptySpliterator<Integer, TSpliterator.OfInt, IntConsumer>
                implements TSpliterator.OfInt {

            OfInt() { }
        }

        private static final class OfLong
                extends EmptySpliterator<Long, TSpliterator.OfLong, LongConsumer>
                implements TSpliterator.OfLong {

            OfLong() { }
        }
    }

    private static final class ConsumerIterator<T> implements Consumer<T>, TIterator<T> {
        private final TSpliterator<? extends T> spliterator;
        private T nextElement;
        private boolean hasElement;

        ConsumerIterator(TSpliterator<? extends T> spliterator) {
            this.spliterator = Objects.requireNonNull(spliterator);
        }

        @Override
        public void accept(T element) {
            nextElement = element;
        }

        @Override
        public boolean hasNext() {
            if (!hasElement) {
                hasElement = spliterator.tryAdvance(this);
            }
            return hasElement;
        }

        @Override
        public T next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            hasElement = false;
            T element = nextElement;
            nextElement = null;
            return element;
        }
    }

    private static final class DoubleConsumerIterator
            implements DoubleConsumer, TPrimitiveIterator.OfDouble {

        private final TSpliterator.OfDouble spliterator;
        private double nextElement;
        private boolean hasElement;

        DoubleConsumerIterator(TSpliterator.OfDouble spliterator) {
            this.spliterator = Objects.requireNonNull(spliterator);
        }

        @Override
        public void accept(double d) {
            nextElement = d;
        }

        @Override
        public boolean hasNext() {
            if (!hasElement) {
                hasElement = spliterator.tryAdvance(this);
            }
            return hasElement;
        }

        @Override
        public double nextDouble() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            hasElement = false;
            return nextElement;
        }
    }

    private static final class IntConsumerIterator
            implements IntConsumer, TPrimitiveIterator.OfInt {

        private final TSpliterator.OfInt spliterator;
        private int nextElement;
        private boolean hasElement;

        IntConsumerIterator(TSpliterator.OfInt spliterator) {
            this.spliterator = Objects.requireNonNull(spliterator);
        }

        @Override
        public void accept(int i) {
            nextElement = i;
        }

        @Override
        public boolean hasNext() {
            if (!hasElement) {
                hasElement = spliterator.tryAdvance(this);
            }
            return hasElement;
        }

        @Override
        public int nextInt() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            hasElement = false;
            return nextElement;
        }
    }

    private static final class LongConsumerIterator
            implements LongConsumer, TPrimitiveIterator.OfLong {

        private final TSpliterator.OfLong spliterator;
        private long nextElement;
        private boolean hasElement;

        LongConsumerIterator(TSpliterator.OfLong spliterator) {
            this.spliterator = Objects.requireNonNull(spliterator);
        }

        @Override
        public void accept(long l) {
            nextElement = l;
        }

        @Override
        public boolean hasNext() {
            if (!hasElement) {
                hasElement = spliterator.tryAdvance(this);
            }
            return hasElement;
        }

        @Override
        public long nextLong() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            hasElement = false;
            return nextElement;
        }
    }

    static class IteratorSpliterator<T> implements TSpliterator<T> {
        private TCollection<? extends T> collection;
        private TIterator<? extends T> it;
        private final int characteristics;
        private long estimateSize;

        IteratorSpliterator(TCollection<? extends T> collection, int characteristics) {
            this.collection = Objects.requireNonNull(collection);
            this.characteristics = sizeKnownIteratorSpliteratorCharacteristics(characteristics);
        }

        IteratorSpliterator(TIterator<? extends T> it, long size, int characteristics) {
            this.it = Objects.requireNonNull(it);
            this.characteristics = sizeKnownIteratorSpliteratorCharacteristics(characteristics);
            this.estimateSize = size;
        }

        IteratorSpliterator(TIterator<? extends T> it, int characteristics) {
            this.it = Objects.requireNonNull(it);
            this.characteristics = sizeUnknownSpliteratorCharacteristics(characteristics);
            this.estimateSize = Long.MAX_VALUE;
        }

        @Override
        public int characteristics() {
            return characteristics;
        }

        @Override
        public long estimateSize() {
            initIterator();
            return estimateSize;
        }

        @Override
        public void forEachRemaining(Consumer<? super T> consumer) {
            initIterator();
            it.forEachRemaining(consumer);
        }

        @Override
        public TComparator<? super T> getComparator() {
            checkSorted(characteristics);
            return null;
        }

        @Override
        public boolean tryAdvance(Consumer<? super T> consumer) {
            Objects.requireNonNull(consumer);
            initIterator();
            if (it.hasNext()) {
                consumer.accept(it.next());
                return true;
            }
            return false;
        }

        @Override
        public TSpliterator<T> trySplit() {
            // see javadoc for java.util.Spliterator
            return null;
        }

        private void initIterator() {
            if (it == null) {
                it = collection.iterator();
                estimateSize = (long) collection.size();
            }
        }
    }

    private static final class DoubleIteratorSpliterator extends AbstractDoubleSpliterator {
        private final TPrimitiveIterator.OfDouble it;

        DoubleIteratorSpliterator(TPrimitiveIterator.OfDouble it, long size, int characteristics) {
            super(size, sizeKnownIteratorSpliteratorCharacteristics(characteristics));
            this.it = Objects.requireNonNull(it);
        }

        DoubleIteratorSpliterator(TPrimitiveIterator.OfDouble it, int characteristics) {
            super(Long.MAX_VALUE, sizeUnknownSpliteratorCharacteristics(characteristics));
            this.it = Objects.requireNonNull(it);
        }

        @Override
        public void forEachRemaining(DoubleConsumer consumer) {
            it.forEachRemaining(consumer);
        }

        @Override
        public TComparator<? super Double> getComparator() {
            checkSorted(characteristics());
            return null;
        }

        @Override
        public boolean tryAdvance(DoubleConsumer consumer) {
            Objects.requireNonNull(consumer);
            if (it.hasNext()) {
                consumer.accept(it.nextDouble());
                return true;
            }
            return false;
        }
    }

    private static final class IntIteratorSpliterator extends AbstractIntSpliterator {
        private final TPrimitiveIterator.OfInt it;

        IntIteratorSpliterator(TPrimitiveIterator.OfInt it, long size, int characteristics) {
            super(size, sizeKnownIteratorSpliteratorCharacteristics(characteristics));
            this.it = Objects.requireNonNull(it);
        }

        IntIteratorSpliterator(TPrimitiveIterator.OfInt it, int characteristics) {
            super(Long.MAX_VALUE, sizeUnknownSpliteratorCharacteristics(characteristics));
            this.it = Objects.requireNonNull(it);
        }

        @Override
        public void forEachRemaining(IntConsumer consumer) {
            it.forEachRemaining(consumer);
        }

        @Override
        public TComparator<? super Integer> getComparator() {
            checkSorted(characteristics());
            return null;
        }

        @Override
        public boolean tryAdvance(IntConsumer consumer) {
            Objects.requireNonNull(consumer);
            if (it.hasNext()) {
                consumer.accept(it.nextInt());
                return true;
            }
            return false;
        }
    }

    private static final class LongIteratorSpliterator extends AbstractLongSpliterator {
        private final TPrimitiveIterator.OfLong it;

        LongIteratorSpliterator(TPrimitiveIterator.OfLong it, long size, int characteristics) {
            super(size, sizeKnownIteratorSpliteratorCharacteristics(characteristics));
            this.it = Objects.requireNonNull(it);
        }

        LongIteratorSpliterator(TPrimitiveIterator.OfLong it, int characteristics) {
            super(Long.MAX_VALUE, sizeUnknownSpliteratorCharacteristics(characteristics));
            this.it = Objects.requireNonNull(it);
        }

        @Override
        public void forEachRemaining(LongConsumer consumer) {
            it.forEachRemaining(consumer);
        }

        @Override
        public TComparator<? super Long> getComparator() {
            checkSorted(characteristics());
            return null;
        }

        @Override
        public boolean tryAdvance(LongConsumer consumer) {
            Objects.requireNonNull(consumer);
            if (it.hasNext()) {
                consumer.accept(it.nextLong());
                return true;
            }
            return false;
        }
    }

    private abstract static class BaseArraySpliterator<T, S extends TSpliterator<T>, C>
            implements TSpliterator<T> {
        private int index;
        private final int limit;
        private final int characteristics;

        BaseArraySpliterator(int from, int limit, int characteristics) {
            this.index = from;
            this.limit = limit;
            this.characteristics = sizeKnownSpliteratorCharacteristics(characteristics);
        }

        public int characteristics() {
            return characteristics;
        }

        public long estimateSize() {
            return limit - index;
        }

        public void forEachRemaining(C consumer) {
            Objects.requireNonNull(consumer);
            while (index < limit) {
                consume(consumer, index++);
            }
        }

        public TComparator<? super T> getComparator() {
            checkSorted(characteristics);
            return null;
        }

        public boolean tryAdvance(C consumer) {
            Objects.requireNonNull(consumer);
            if (index < limit) {
                consume(consumer, index++);
                return true;
            }
            return false;
        }

        public S trySplit() {
            // see javadoc for java.util.Spliterator
            return null;
        }

        protected abstract void consume(C consumer, int index);
    }

    private static final class ArraySpliterator<T>
            extends BaseArraySpliterator<T, TSpliterator<T>, Consumer<? super T>>
            implements TSpliterator<T> {

        private final Object[] array;

        ArraySpliterator(Object[] array, int characteristics) {
            this(array, 0, array.length, characteristics);
        }

        ArraySpliterator(Object[] array, int from, int limit, int characteristics) {
            super(from, limit, characteristics);
            this.array = array;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void consume(Consumer<? super T> consumer, int index) {
            consumer.accept((T) array[index]);
        }
    }

    private static final class DoubleArraySpliterator
            extends BaseArraySpliterator<Double, TSpliterator.OfDouble, DoubleConsumer>
            implements TSpliterator.OfDouble {

        private final double[] array;

        DoubleArraySpliterator(double[] array, int characteristics) {
            this(array, 0, array.length, characteristics);
        }

        DoubleArraySpliterator(double[] array, int from, int limit, int characteristics) {
            super(from, limit, characteristics);
            this.array = array;
        }

        @Override
        protected void consume(DoubleConsumer consumer, int index) {
            consumer.accept(array[index]);
        }
    }

    private static final class IntArraySpliterator
            extends BaseArraySpliterator<Integer, TSpliterator.OfInt, IntConsumer>
            implements TSpliterator.OfInt {

        private final int[] array;

        IntArraySpliterator(int[] array, int characteristics) {
            this(array, 0, array.length, characteristics);
        }

        IntArraySpliterator(int[] array, int from, int limit, int characteristics) {
            super(from, limit, characteristics);
            this.array = array;
        }

        @Override
        protected void consume(IntConsumer consumer, int index) {
            consumer.accept(array[index]);
        }
    }

    private static final class LongArraySpliterator
            extends BaseArraySpliterator<Long, TSpliterator.OfLong, LongConsumer>
            implements TSpliterator.OfLong {

        private final long[] array;

        LongArraySpliterator(long[] array, int characteristics) {
            this(array, 0, array.length, characteristics);
        }

        LongArraySpliterator(long[] array, int from, int limit, int characteristics) {
            super(from, limit, characteristics);
            this.array = array;
        }

        @Override
        protected void consume(LongConsumer consumer, int index) {
            consumer.accept(array[index]);
        }
    }

    private static void checkSorted(int characteristics) {
        if ((characteristics & TSpliterator.SORTED) == 0) {
            throw new IllegalStateException();
        }
    }

    private static int sizeKnownSpliteratorCharacteristics(int characteristics) {
        return characteristics | TSpliterator.SIZED | TSpliterator.SUBSIZED;
    }

    private static int sizeKnownIteratorSpliteratorCharacteristics(int characteristics) {
        return (characteristics & TSpliterator.CONCURRENT) == 0
                ? sizeKnownSpliteratorCharacteristics(characteristics) : characteristics;
    }

    private static int sizeUnknownSpliteratorCharacteristics(int characteristics) {
        return characteristics & ~(TSpliterator.SIZED | TSpliterator.SUBSIZED);
    }

    /**
     * We cant use InternalPreconditions.checkCriticalArrayBounds here because
     * Spliterators must throw only ArrayIndexOutOfBoundsException on range check by contract.
     */
    private static void checkCriticalArrayBounds(int start, int end, int length) {
        if (start > end || start < 0 || end > length) {
            throw new ArrayIndexOutOfBoundsException(
                    "fromIndex: " + start + ", toIndex: " + end + ", length: " + length);
        }
    }

    private TSpliterators() { }

}

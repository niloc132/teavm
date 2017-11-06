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

import org.teavm.classlib.java.util.TIterator;
import org.teavm.classlib.java.util.TSpliterator;

/**
 * See <a href="https://docs.oracle.com/javase/8/docs/api/java/util/stream/BaseStream.html">
 * the official Java API doc</a> for details.
 *
 * @param <T> the contents of the stream
 * @param <S> the type of stream implementing this interface
 */
public interface TBaseStream<T, S extends TBaseStream<T, S>> extends AutoCloseable {
    TIterator<T> iterator();

    TSpliterator<T> spliterator();

    boolean isParallel();

    S sequential();

    S parallel();

    S unordered();

    S onClose(Runnable closeHandler);

    @Override
    void close();
}

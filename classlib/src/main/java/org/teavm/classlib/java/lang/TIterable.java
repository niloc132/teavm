/*
 *  Copyright 2014 Alexey Andreev.
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
package org.teavm.classlib.java.lang;

import org.teavm.classlib.java.util.TIterator;
import org.teavm.classlib.java.util.TSpliterator;
import org.teavm.classlib.java.util.TSpliterators;
import org.teavm.classlib.java.util.function.TConsumer;

/**
 *
 * @author Alexey Andreev
 * @param <T> type this collection returns.
 */
public interface TIterable<T> {
    TIterator<T> iterator();

    default TSpliterator<T> spliterator() {
        return TSpliterators.spliteratorUnknownSize(iterator(), 0);
    }

    default void forEach(TConsumer<? super T> consumer) {
        for (TIterator<T> it = iterator(); it.hasNext();) {
            consumer.accept(it.next());
        }
    }
}

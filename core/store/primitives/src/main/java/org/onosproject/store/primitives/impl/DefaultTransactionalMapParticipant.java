/*
 * Copyright 2017-present Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.store.primitives.impl;

import java.util.Map;
import java.util.stream.Stream;

import com.google.common.collect.Maps;
import org.apache.commons.lang3.tuple.Pair;
import org.onosproject.store.primitives.MapUpdate;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.Versioned;

/**
 * Repeatable read based map participant.
 */
public class DefaultTransactionalMapParticipant<K, V> extends TransactionalMapParticipant<K, V> {
    private final Map<K, Versioned<V>> readCache = Maps.newConcurrentMap();

    public DefaultTransactionalMapParticipant(
            ConsistentMap<K, V> backingMap, Transaction<MapUpdate<K, V>> transaction) {
        super(backingMap, transaction);
    }

    @Override
    protected V read(K key) {
        Versioned<V> value = readCache.computeIfAbsent(key, backingMap::get);
        return value != null ? value.value() : null;
    }

    @Override
    protected Stream<MapUpdate<K, V>> records() {
        return Stream.concat(deleteStream(), writeStream());
    }

    /**
     * Returns a transaction record stream for deleted keys.
     */
    private Stream<MapUpdate<K, V>> deleteStream() {
        return deleteSet.stream()
                .map(key -> Pair.of(key, readCache.get(key)))
                .filter(e -> e.getValue() != null)
                .map(e -> MapUpdate.<K, V>newBuilder()
                        .withType(MapUpdate.Type.REMOVE_IF_VERSION_MATCH)
                        .withKey(e.getKey())
                        .withCurrentVersion(e.getValue().version())
                        .build());
    }

    /**
     * Returns a transaction record stream for updated keys.
     */
    private Stream<MapUpdate<K, V>> writeStream() {
        return writeCache.entrySet().stream().map(entry -> {
            Versioned<V> original = readCache.get(entry.getKey());
            if (original == null) {
                return MapUpdate.<K, V>newBuilder()
                        .withType(MapUpdate.Type.PUT_IF_ABSENT)
                        .withKey(entry.getKey())
                        .withValue(entry.getValue())
                        .build();
            } else {
                return MapUpdate.<K, V>newBuilder()
                        .withType(MapUpdate.Type.PUT_IF_VERSION_MATCH)
                        .withKey(entry.getKey())
                        .withCurrentVersion(original.version())
                        .withValue(entry.getValue())
                        .build();
            }
        });
    }
}
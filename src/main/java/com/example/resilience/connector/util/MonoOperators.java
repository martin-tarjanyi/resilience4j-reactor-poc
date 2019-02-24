package com.example.resilience.connector.util;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

public class MonoOperators
{
    public static <T, V> Function<? super Mono<T>, ? extends Publisher<V>> mapWithContext(
            BiFunction<T, Context, V> mapper)
    {
        return mono -> mono.flatMap(t -> Mono.subscriberContext().map(context -> mapper.apply(t, context)));
    }

    public static <T> Function<? super Mono<T>, ? extends Publisher<T>> doWithContext(BiConsumer<T, Context> consumer)
    {
        return mono -> mono.flatMap(
                t -> Mono.subscriberContext()
                         .doOnNext(context -> consumer.accept(t, context))
                         .map(context -> t)
        );
    }
}

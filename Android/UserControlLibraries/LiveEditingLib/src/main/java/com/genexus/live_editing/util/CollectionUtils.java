package com.genexus.live_editing.util;

import java.util.ArrayList;
import java.util.List;

import android.support.annotation.NonNull;

public class CollectionUtils {
    public static <T> List<T> filter(@NonNull Iterable<T> iterable, @NonNull Predicate<T> predicate) {
        List<T> result = new ArrayList<>();

        for (T element : iterable) {
            if (predicate.apply(element)) {
                result.add(element);
            }
        }

        return result;
    }

    public interface Predicate<T> {
        boolean apply(@NonNull T input);
    }
}
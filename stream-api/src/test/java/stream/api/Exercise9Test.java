package stream.api;

import common.test.tool.annotation.Difficult;
import common.test.tool.annotation.Easy;
import common.test.tool.dataset.ClassicOnlineStore;
import common.test.tool.entity.Customer;
import common.test.tool.util.CollectorImpl;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.IntStream;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

public class Exercise9Test extends ClassicOnlineStore {

    @Easy @Test
    public void simplestStringJoin() {
        List<Customer> customerList = this.mall.getCustomerList();

        /**
         * Implement a {@link Collector} which can create a String with comma separated names shown in the assertion.
         * The collector will be used by serial stream.
         */
        Supplier<StringBuilder> supplier = StringBuilder::new;
        BiConsumer<StringBuilder, String> accumulator = (a, v) -> a.append(v).append(',');
        BinaryOperator<StringBuilder> combiner = (a, b) -> a.append(b);
        Function<StringBuilder, String> finisher = a -> a.deleteCharAt(a.length() - 1).toString();

        Collector<String, ?, String> toCsv =
            new CollectorImpl<>(supplier, accumulator, combiner, finisher, Collections.emptySet());
        String nameAsCsv = customerList.stream().map(Customer::getName).collect(toCsv);
        assertThat(nameAsCsv, is("Joe,Steven,Patrick,Diana,Chris,Kathy,Alice,Andrew,Martin,Amy"));
    }

    @Difficult @Test
    public void mapKeyedByItems() {
        List<Customer> customerList = this.mall.getCustomerList();

        /**
         * Implement a {@link Collector} which can create a {@link Map} with keys as item and
         * values as {@link Set} of customers who are wanting to buy that item.
         * The collector will be used by parallel stream.
         */
        Supplier<Map<String, Set<String>>> supplier = HashMap<String, Set<String>>::new;
        BiConsumer<Map<String, Set<String>>, Customer> accumulator = (map, customer) ->
            customer.getWantToBuy().forEach(item -> {
                map.computeIfAbsent(
                    item.getName(),
                    key -> new HashSet<String>()
                )
                .add(customer.getName());
            });
        BinaryOperator<Map<String, Set<String>>> combiner = (mapA, mapB) -> {
            mapB.forEach((k, v) ->
                    mapA.computeIfAbsent(k, key -> new HashSet<String>()).addAll(v)
            );
            return mapA;
        };
        Function<Map<String, Set<String>>, Map<String, Set<String>>> finisher = Function.identity();

        Collector<Customer, ?, Map<String, Set<String>>> toItemAsKey =
            new CollectorImpl<>(supplier, accumulator, combiner, finisher, EnumSet.of(
                Collector.Characteristics.CONCURRENT,
                Collector.Characteristics.IDENTITY_FINISH));
        Map<String, Set<String>> itemMap = customerList.stream().parallel().collect(toItemAsKey);
        assertThat(itemMap.get("plane"), containsInAnyOrder("Chris"));
        assertThat(itemMap.get("onion"), containsInAnyOrder("Patrick", "Amy"));
        assertThat(itemMap.get("ice cream"), containsInAnyOrder("Patrick", "Steven"));
        assertThat(itemMap.get("earphone"), containsInAnyOrder("Steven"));
        assertThat(itemMap.get("plate"), containsInAnyOrder("Joe", "Martin"));
        assertThat(itemMap.get("fork"), containsInAnyOrder("Joe", "Martin"));
        assertThat(itemMap.get("cable"), containsInAnyOrder("Diana", "Steven"));
        assertThat(itemMap.get("desk"), containsInAnyOrder("Alice"));
    }

    @Difficult @Test
    public void bitList2BitString() {
        String bitList = "22-24,9,42-44,11,4,46,14-17,5,2,38-40,33,50,48";

        /**
         * Create a {@link String} of "n"th bit ON.
         * for example
         * "3" will be "001"
         * "1,3,5" will be "10101"
         * "1-3" will be "111"
         * "7,1-3,5" will be "1110101"
         */

        Supplier<StringBuilder> supplier = StringBuilder::new;

        BiConsumer<StringBuilder, String> accumulator = (builder, str) -> {
            final int oldLength = builder.length();
            int fromIdx, toIdx;
            if (str.indexOf('-') >= 0) {
                final String[] split = str.split("-");
                fromIdx = Integer.parseInt(split[0]) - 1;
                toIdx = Integer.parseInt(split[1]);
            }
            else {
                toIdx = Integer.parseInt(str);
                fromIdx = toIdx - 1;
            }

            if (oldLength < toIdx) {
                builder.setLength(toIdx);
                IntStream.range(oldLength, toIdx).forEach(i -> builder.setCharAt(i, '0'));
            }
            IntStream.range(fromIdx, toIdx).forEach(i -> builder.setCharAt(i, '1'));
        };

        BinaryOperator<StringBuilder> combiner = (sb1, sb2) -> {
            final StringBuilder longer  = sb1.length() > sb2.length() ? sb1 : sb2;
            final StringBuilder shorter = sb1.length() > sb2.length() ? sb2 : sb1;
            IntStream.range(0, shorter.length()).forEach(i ->
                longer.setCharAt(i, shorter.charAt(i) == '1' ? '1' : longer.charAt(i))
            );
            return longer;
        };

        Function<StringBuilder, String> finisher = StringBuilder::toString;

        Collector<String, ?, String> toBitString =
            new CollectorImpl<>(supplier, accumulator, combiner, finisher, Collections.emptySet());

        String bitString = Arrays.stream(bitList.split(",")).collect(toBitString);
        assertThat(bitString, is("01011000101001111000011100000000100001110111010101")

        );
    }
}

package mthiebi.sgs.repository;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.*;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.apache.commons.lang3.StringUtils;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.stream.Stream;

public final class QueryUtils {

    public static BooleanExpression where(BooleanExpression... predicates) {
        return Optional.ofNullable(predicates)
                       .map(Arrays::asList)
                       .orElseGet(List::of)
                       .stream()
                       .reduce(True(), BooleanExpression::and);
    }

    public static BooleanExpression or(BooleanExpression... predicates) {
        return Optional.ofNullable(predicates)
                       .map(Arrays::asList)
                       .filter(list -> !list.isEmpty())
                       .orElseGet(() -> List.of(True()))
                       .stream()
                       .reduce(False(), BooleanExpression::or);
    }

    public static OrderSpecifier<?>[] orderBy(OrderSpecifier<?>... orderSpecifiers) {
        return orderSpecifiers;
    }

    public static ComparableExpressionBase<?>[] groupBy(ComparableExpressionBase<?>... predicates) {
        return Stream.of(predicates)
                     .filter(Objects::nonNull)
                     .toArray(ComparableExpressionBase<?>[]::new);
    }

    public static BooleanExpression boolEq(BooleanPath booleanField, Boolean b) {
        return b == null ? True() : booleanField.eq(b);
    }

    public static BooleanExpression stringEq(StringPath stringField, String s) {
        return StringUtils.isBlank(s) ? True() : stringField.eq(s.trim());
    }

    public static BooleanExpression stringNotBlankAndEquals(StringPath stringField, String s) {
        return StringUtils.isBlank(s) ? False() : stringField.eq(s.trim());
    }

    public static BooleanExpression stringLike(StringPath stringField, String s) {
        return StringUtils.isBlank(s) ? True() : stringField.toLowerCase().like('%' + s.toLowerCase().trim() + '%');
    }

    public static BooleanExpression customStringLike(StringPath stringField, String s) {
        return StringUtils.isBlank(s) ? True() : stringField.toLowerCase().like(s.toLowerCase().trim());
    }

    public static BooleanExpression stringMore(StringPath stringField, String s) {
        return StringUtils.isBlank(s) ? True() : stringField.toLowerCase().gt(s);
    }

    public static BooleanExpression stringMoreOrEq(StringPath stringField, String s) {
        return StringUtils.isBlank(s) ? True() : stringField.toLowerCase().goe(s);
    }

    public static BooleanExpression stringLess(StringPath stringField, String s) {
        return StringUtils.isBlank(s) ? True() : stringField.toLowerCase().lt(s);
    }

    public static BooleanExpression stringLessOrEq(StringPath stringField, String s) {
        return StringUtils.isBlank(s) ? True() : stringField.toLowerCase().loe(s);
    }

    public static BooleanExpression stringIn(StringPath stringField, List<String> stringValues) {
        return stringValues == null ? True() : stringField.in(stringValues);
    }

    public static BooleanExpression intEq(NumberPath<Integer> intField, Integer i) {
        return i == null ? True() : intField.eq(i);
    }

    public static BooleanExpression intLess(NumberPath<Integer> intField, Integer i) {
        return i == null ? True() : intField.lt(i);
    }

    public static BooleanExpression intMore(NumberPath<Integer> intField, Integer i) {
        return i == null ? True() : intField.gt(i);
    }

    public static BooleanExpression intMoreOrEq(NumberPath<Integer> intField, Integer i) {
        return i == null ? True() : intField.goe(i);
    }

    public static BooleanExpression intLessOrEq(NumberPath<Integer> intField, Integer i) {
        return i == null ? True() : intField.loe(i);
    }

    public static BooleanExpression longIs(NumberPath<Long> longField, Long l) {
        return l == null ? False() : longField.eq(l);
    }

    public static BooleanExpression longEq(NumberPath<Long> longField, Long l) {
        return l == null ? True() : longField.eq(l);
    }

    public static BooleanExpression longLess(NumberPath<Long> longField, Long l) {
        return l == null ? True() : longField.lt(l);
    }

    public static BooleanExpression longMore(NumberPath<Long> longField, Long l) {
        return l == null ? True() : longField.gt(l);
    }

    public static BooleanExpression longMoreOrEq(NumberPath<Long> longField, Long l) {
        return l == null ? True() : longField.goe(l);
    }

    public static BooleanExpression longLessOrEq(NumberPath<Long> longField, Long l) {
        return l == null ? True() : longField.loe(l);
    }

    public static BooleanExpression longIn(NumberPath<Long> longField, List<Long> longs) {
        return longs == null ? True() : longField.in(longs);
    }

    public static BooleanExpression decimalEq(NumberPath<BigDecimal> decimalField, BigDecimal l) {
        return l == null ? True() : decimalField.eq(l);
    }

    public static BooleanExpression decimalLess(NumberPath<BigDecimal> decimalField, BigDecimal l) {
        return l == null ? True() : decimalField.lt(l);
    }

    public static BooleanExpression decimalMore(NumberPath<BigDecimal> decimalField, BigDecimal l) {
        return l == null ? True() : decimalField.gt(l);
    }

    public static BooleanExpression decimalMoreOrEq(NumberPath<BigDecimal> decimalField, BigDecimal l) {
        return l == null ? True() : decimalField.goe(l);
    }

    public static BooleanExpression decimalLessOrEq(NumberPath<BigDecimal> decimalField, BigDecimal l) {
        return l == null ? True() : decimalField.loe(l);
    }

    public static BooleanExpression dateTimeEq(DateTimePath<Date> timeField, Date time) {
        return time == null ? True() : timeField.eq(time);
    }

    public static BooleanExpression dateTimeLess(DateTimePath<Date> timeField, Date time) {
        return time == null ? True() : timeField.before(time);
    }

    public static BooleanExpression dateTimeLessOrEq(DateTimePath<Date> timeField, Date time) {
        return time == null ? True() : timeField.before(time).or(timeField.eq(time));
    }

    public static BooleanExpression dateTimeMore(DateTimePath<Date> timeField, Date time) {
        return time == null ? True() : timeField.after(time);
    }

    public static BooleanExpression dateTimeMoreOrEq(DateTimePath<Date> timeField, Date time) {
        return time == null ? True() : timeField.after(time).or(timeField.eq(time));
    }
    
    public static BooleanExpression dateEq(DatePath<Date> timeField, Date time) {
        return time == null ? True() : timeField.eq(time);
    }

    public static BooleanExpression dateLess(DatePath<Date> timeField, Date time) {
        return time == null ? True() : timeField.before(time);
    }

    public static BooleanExpression dateLessOrEq(DatePath<Date> timeField, Date time) {
        return time == null ? True() : timeField.before(time).or(timeField.eq(time));
    }

    public static BooleanExpression dateMore(DatePath<Date> timeField, Date time) {
        return time == null ? True() : timeField.after(time);
    }

    public static BooleanExpression dateMoreOrEq(DatePath<Date> timeField, Date time) {
        return time == null ? True() : timeField.after(time).or(timeField.eq(time));
    }

    public static BooleanExpression localDateEq(DatePath<LocalDate> timeField, LocalDate time) {
        return time == null ? True() : timeField.eq(time);
    }

    public static BooleanExpression localDateLess(DatePath<LocalDate> timeField, LocalDate time) {
        return time == null ? True() : timeField.before(time);
    }

    public static BooleanExpression localDateLessOrEq(DatePath<LocalDate> timeField, LocalDate time) {
        return time == null ? True() : timeField.before(time).or(timeField.eq(time));
    }

    public static BooleanExpression localDateMore(DatePath<LocalDate> timeField, LocalDate time) {
        return time == null ? True() : timeField.after(time);
    }

    public static BooleanExpression localDateMoreOrEq(DatePath<LocalDate> timeField, LocalDate time) {
        return time == null ? True() : timeField.after(time).or(timeField.eq(time));
    }

    public static BooleanExpression in(StringPath stringField, Collection<String> currencies) {
        return currencies == null ? True() : stringField.in(currencies);
    }

    public static BooleanExpression in(NumberPath<Long> idField, Collection<Long> ids) {
        return ids == null ? True() : idField.in(ids);
    }

    public static <T extends Enum<T>> BooleanExpression enumEq(EnumPath<T> enumField, T enumValue) {
        return enumValue == null ? True() : enumField.eq(enumValue);
    }


    public static <T extends Enum<T>> BooleanExpression enumNotEq(EnumPath<T> enumField, T enumValue) {
        return enumValue == null ? True() : enumField.ne(enumValue);
    }

    public static <T extends Enum<T>> BooleanExpression enumIn(EnumPath<T> enumField, List<T> enumValues) {
        return enumValues == null ? True() : enumField.in(enumValues);
    }

    public static BooleanExpression stringEqKeyValueForMapAttributeToJson(MapPath<String, String, StringPath> mapPath, String key, String value) {
        return StringUtils.isBlank(key) && StringUtils.isBlank(value) ? True() :
               CastToString(mapPath).toLowerCase().like("%\"" + key.toLowerCase().trim() + "\":%").and(
                       CastToString(mapPath).toLowerCase().like("%:\"" + value.toLowerCase().trim() + "\"%")
               );
    }


    public static BooleanExpression stringEqAnyKeyValueForMapAttributeToJson(MapPath<String, String, StringPath> mapPath, Map<String, String> map) {
        BooleanExpression eqAny = False();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            eqAny = eqAny.or(stringEqKeyValueForMapAttributeToJson(mapPath, entry.getKey(), entry.getValue()));
        }
        return eqAny;
    }

    public static BooleanExpression StringEqValueForMapAttributeToJson(MapPath<String, String, StringPath> mapPath, String s) {
        return StringUtils.isBlank(s) ? True() : CastToString(mapPath).toLowerCase().like("%:\"" + s.toLowerCase().trim() + "\"%");
    }

    public static BooleanExpression StringLikeForMapAttributeToJson(MapPath<String, String, StringPath> mapPath, String s) {
        return StringUtils.isBlank(s) ? True() : CastToString(mapPath).toLowerCase().like("%:\"%" + s.toLowerCase().trim() + "%\"%")
                                                                      .or(CastToString(mapPath).toLowerCase().like("%:\"%" + s.toLowerCase().trim().replaceAll("\"", Matcher.quoteReplacement("\\\"")) + "%\"%"));
    }

    public static BooleanExpression StringInForMapAttributeToJson(MapPath<String, String, StringPath> mapPath, List<String> stringList) {
        return stringList == null ? True() : stringList.stream()
                                                       .map(s -> CastToString(mapPath).toLowerCase().like("%:\"" + s.toLowerCase().trim() + "\"%"))
                                                       .reduce(False(), BooleanExpression::or);
    }

    public static StringExpression CastToString(Object object) {
        return Expressions.stringTemplate("CAST({0} AS string)", object);
    }

    @NotNull
    public static Long getCount(JPAQueryFactory qf, ComparableExpressionBase<?> idPath, Predicate where) {
        EntityPathBase<?> entityPath = (EntityPathBase<?>) ((Path<?>) idPath).getMetadata().getParent();
        return qf.select(idPath.count())
                 .from(entityPath)
                 .where(where)
                 .fetchOne();
    }

    public static <T> List<T> toList(Iterable<T> iterable) {
        List<T> result = new ArrayList<>();
        for (T t : iterable) {
            result.add(t);
        }
        return result;
    }

    public static BooleanExpression True() {
        return Expressions.asBoolean(true).isTrue();
    }

    public static BooleanExpression False() {
        return Expressions.asBoolean(true).isFalse();
    }
}

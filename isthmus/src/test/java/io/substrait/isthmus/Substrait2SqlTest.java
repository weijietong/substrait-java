package io.substrait.isthmus;

import static io.substrait.isthmus.SqlToSubstrait.EXTENSION_COLLECTION;

import io.substrait.relation.Rel;
import java.util.Arrays;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.RelRoot;
import org.apache.calcite.sql.SqlKind;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class Substrait2SqlTest extends PlanTestBase {
  @Test
  public void simpleTest() throws Exception {
    String query = "select p_size  from part where p_partkey > cast(100 as bigint)";
    test(query);
  }

  @Test
  public void simpleTest2() throws Exception {
    String query =
        "select l_partkey, l_discount from lineitem where l_orderkey > cast(100 as bigint)";
    test(query);
  }

  @Test
  public void simpleTestDateInterval() throws Exception {
    test(
        "select l_partkey + l_orderkey, l_shipdate from lineitem where l_shipdate < date '1998-01-01' ");
    test(
        "select l_partkey + l_orderkey, l_shipdate from lineitem where l_shipdate < date '1998-01-01' + interval '3' month ");
    test(
        "select l_partkey + l_orderkey, l_shipdate from lineitem where l_shipdate < date '1998-01-01' + interval '1' year");
    test(
        "select l_partkey + l_orderkey, l_shipdate from lineitem where l_shipdate < date '1998-01-01' + interval '1-3' year to month");
  }

  @Test
  public void simpleTestDecimal() throws Exception {
    test(
        "select l_partkey + l_orderkey, l_extendedprice * 0.1 + 100.0 from lineitem where l_shipdate < date '1998-01-01' ");
  }

  private void test(String query) throws Exception {
    String[] values = asString("tpch/schema.sql").split(";");
    var creates = Arrays.stream(values).filter(t -> !t.trim().isBlank()).toList();

    // 1. sql -> substrait rel
    SqlToSubstrait s = new SqlToSubstrait();
    RelRoot relRoot = s.sqlToRelNode(query, creates);
    Rel pojoRel = SubstraitRelVisitor.convert(relRoot, EXTENSION_COLLECTION);

    // 2. substrait rel -> Calcite Rel
    RelNode relnodeRoot = new SubstraitToSql().substraitRelToCalciteRel(pojoRel, creates);

    // 3. Calcite Rel -> substrait rel
    Rel pojoRel2 =
        SubstraitRelVisitor.convert(RelRoot.of(relnodeRoot, SqlKind.SELECT), EXTENSION_COLLECTION);

    Assertions.assertEquals(pojoRel, pojoRel2);
    // 4. Calcite Rel -> sql
    System.out.println(SubstraitToSql.toSql(relnodeRoot));
  }
}

package com.pci.energy.backend.assignment.repository;

import com.pci.energy.backend.assignment.business.Order;
import com.pci.energy.backend.assignment.configuration.AssignmentProperties;
import org.apache.spark.sql.Column;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.catalyst.analysis.NoSuchTableException;
import org.apache.spark.sql.catalyst.analysis.TableAlreadyExistsException;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.Metadata;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import scala.collection.mutable.ArraySeq;

import java.util.Collections;
import java.util.List;

@Component
public class OrderIcebergRepository {
    private static final String TABLE_NAME = "ordering";

    private static final StructType ORDER_SCHEMA = new StructType(new StructField[]{
            new StructField("order_id", DataTypes.IntegerType, false, Metadata.empty()),
            new StructField("customer_id", DataTypes.StringType, false, Metadata.empty()),
            new StructField("product", DataTypes.StringType, false, Metadata.empty()),
            new StructField("category", DataTypes.StringType, false, Metadata.empty()),
            new StructField("quantity", DataTypes.IntegerType, false, Metadata.empty()),
            new StructField("unit_price", DataTypes.FloatType, false, Metadata.empty()),
            new StructField("order_date", DataTypes.DateType, false, Metadata.empty()),
            new StructField("country", DataTypes.StringType, false, Metadata.empty())
    });
    private final SparkSession spark;

    private final String catalog;

    private final String namespace;

    private final String fullTableName;

    private final AssignmentProperties props;

    @Autowired
    public OrderIcebergRepository(SparkSession spark, AssignmentProperties props) {
        this.props = props;
        this.spark = spark;
        this.catalog = props.iceberg().catalogName();
        this.namespace = props.iceberg().defaultNamespace();
        this.fullTableName = "%s.%s.%s".formatted(catalog, namespace, TABLE_NAME);
    }

    public void initializeIcebergStructureForTable() {
        spark.sql("CREATE NAMESPACE IF NOT EXISTS %s.%s".formatted(catalog, namespace));

        if (!spark.catalog().tableExists(fullTableName)) {
            Dataset<Row> df = spark.createDataFrame(Collections.emptyList(), ORDER_SCHEMA);

            try {
                df.writeTo(fullTableName).using("iceberg").partitionedBy(new Column("order_date"), new ArraySeq<>(0)).create();
            } catch (TableAlreadyExistsException e) {
                e.printStackTrace();
            }
        }
        //DESCRIBE FORMATTED table_name;

    }

    public void upsertOrders(List<Order> orders) {

        try {
            List<String> newIds = orders.stream().map(Order::order_id).map(String::valueOf).toList();
            List<Integer> oldIds = this.findAllById(newIds).stream().map(Order::order_id).toList();
            List<Row> rows = orders.stream()
                    .filter(o -> !oldIds.contains(o.order_id()))
                    .map(o -> RowFactory.create(
                            o.order_id(),
                            o.customer_id(),
                            o.product(),
                            o.category(),
                            o.quantity(),
                            o.unit_price(),
                            o.order_date(),
                            o.country()
                    ))
                    .toList();
            Dataset<Row> stagingDf = spark.createDataFrame(rows, ORDER_SCHEMA);

            stagingDf.writeTo(this.fullTableName).append();
        } catch (NoSuchTableException e) {
            e.printStackTrace();
        }
    }

    public List<Order> findAll() {
        Dataset<Row> df = spark.sql("SELECT order_id, customer_id, product, category, quantity, unit_price, order_date, country FROM %s ORDER BY order_id".formatted(fullTableName));
        return mapRowsToOrders(df.collectAsList());
    }

    public List<Order> findAllById(List<String> orderId) {
        Dataset<Row> df = spark.sql("SELECT order_id, customer_id, product, category, quantity, unit_price, order_date, country FROM %s WHERE order_id in (%s)ORDER BY order_id".formatted(fullTableName, String.join(",", orderId)));
        return mapRowsToOrders(df.collectAsList());
    }

    private List<Order> mapRowsToOrders(List<Row> rows) {
        return rows.stream()
                .map(r -> new Order(
                        r.getInt(0),
                        r.getString(1),
                        r.getString(2),
                        r.getString(3),
                        r.getInt(4),
                        r.getFloat(5),
                        r.getDate(6),
                        r.getString(7)
                ))
                .toList();
    }

    public List<Order> bulk(String fileName) {
        Dataset<Row> df = spark.read()
                .option("header", "true")
                .option("delimiter", ",")
                .schema(ORDER_SCHEMA)
                .csv(this.props.hadoop().defaultFs() + "/bulk/order/" + fileName);

        List<Row> filteredDf = df.collectAsList().stream()
                .filter(r -> !r.isNullAt(0) && !r.isNullAt(1) && !r.isNullAt(2) && !r.isNullAt(3) && !r.isNullAt(4) && !r.isNullAt(5) && !r.isNullAt(6) && !r.isNullAt(7))
                .filter(r -> r.getInt(4) > 0)
                .distinct()
                .toList();
        List<Order> orderList = mapRowsToOrders(filteredDf);
        this.upsertOrders(orderList);
        return orderList;
    }
}

package io.cloudstub.showcase.ingestion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Parses sales report CSV text into {@link SaleRecord} rows. The expected header is {@code
 * date,product,quantity,amount,region,salesperson}; the header row and blank lines are skipped.
 */
@Component
public class CsvParser {

    public List<SaleRecord> parse(String csv) {
        List<SaleRecord> records = new ArrayList<>();
        String[] lines = csv.split("\\r?\\n");
        boolean header = true;
        for (String line : lines) {
            if (line.isBlank()) {
                continue;
            }
            if (header) {
                header = false;
                continue;
            }
            String[] f = line.split(",", -1);
            if (f.length < 6) {
                throw new IllegalArgumentException("Malformed CSV row: " + line);
            }
            records.add(
                    new SaleRecord(
                            LocalDate.parse(f[0].trim()),
                            f[1].trim(),
                            Integer.parseInt(f[2].trim()),
                            new BigDecimal(f[3].trim()),
                            f[4].trim(),
                            f[5].trim()));
        }
        return records;
    }
}

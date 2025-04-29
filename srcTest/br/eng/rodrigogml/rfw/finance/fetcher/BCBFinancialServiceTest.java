package br.eng.rodrigogml.rfw.finance.fetcher;

import java.time.LocalDate;
import java.util.List;

import org.junit.Test;

import br.eng.rodrigogml.rfw.kernel.exceptions.RFWException;

/**
 * Testes unitários para a classe BCBFinancialService. Testa o download e parse dos índices financeiros do Banco Central.
 *
 * Atenção: Este teste acessa serviços externos, depende da disponibilidade da API do BCB.
 * 
 * @author Rodrigo Leitão
 * @since (29 de abr. de 2025)
 */
public class BCBFinancialServiceTest {

  @Test
  public void testDownloadDollarBuy() throws RFWException {
    List<FinancialIndexEntry> series = BCBFinancialService.getDollarBuySeries(
        LocalDate.of(2023, 1, 1),
        LocalDate.of(2023, 6, 1));
    printSeries("Dollar Buy", series);
  }

  @Test
  public void testDownloadDollarSell() throws RFWException {
    List<FinancialIndexEntry> series = BCBFinancialService.getDollarSellSeries(
        LocalDate.of(2023, 1, 1),
        LocalDate.of(2023, 6, 1));
    printSeries("Dollar Sell", series);
  }

  @Test
  public void testDownloadIPCA() throws RFWException {
    List<FinancialIndexEntry> series = BCBFinancialService.getIPCASeries(
        LocalDate.of(2023, 1, 1),
        LocalDate.of(2023, 6, 1));
    printSeries("IPCA", series);
  }

  @Test
  public void testDownloadPoupanca2012() throws RFWException {
    List<FinancialIndexEntry> series = BCBFinancialService.getPoupanca2012Series(
        LocalDate.of(2023, 1, 1),
        LocalDate.of(2023, 6, 1));
    printSeries("Poupança 2012", series);
  }

  @Test
  public void testDownloadSelicDaily() throws RFWException {
    List<FinancialIndexEntry> series = BCBFinancialService.getSelicDailySeries(
        LocalDate.of(2023, 1, 1),
        LocalDate.of(2023, 6, 1));
    printSeries("Selic Diária", series);
  }

  @Test
  public void testDownloadSelicMonthly() throws RFWException {
    List<FinancialIndexEntry> series = BCBFinancialService.getSelicMonthlySeries(
        LocalDate.of(2023, 1, 1),
        LocalDate.of(2023, 6, 1));
    printSeries("Selic Mensal", series);
  }

  private void printSeries(String title, List<FinancialIndexEntry> series) {
    System.out.println("=== " + title + " ===");
    for (FinancialIndexEntry entry : series) {
      System.out.println(entry.getDate() + " -> " + entry.getValue());
    }
    System.out.println();
  }
}

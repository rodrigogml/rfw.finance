package br.eng.rodrigogml.rfw.finance.fetcher;

import java.util.List;

import org.junit.Test;

import br.eng.rodrigogml.rfw.kernel.exceptions.RFWException;

/**
 * Testes unitários para a classe BCBFinancialService. Testa o download e parse dos índices financeiros do Banco Central.
 *
 * Atenção: Este teste acessa serviços externos, depende da disponibilidade da API do BCB.
 *
 * @author Rodrigo
 * @since 2025-04-28
 */
public class BCBFinancialServiceTest {

  @Test
  public void testDownloadDollarBuy() {
    try {
      List<FinancialIndexEntry> series = BCBFinancialService.getDollarBuySeries("01/01/2023", "01/06/2023");
      printSeries("Dollar Buy", series);
    } catch (RFWException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testDownloadDollarSell() {
    try {
      List<FinancialIndexEntry> series = BCBFinancialService.getDollarSellSeries("01/01/2023", "01/06/2023");
      printSeries("Dollar Sell", series);
    } catch (RFWException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testDownloadIPCA() {
    try {
      List<FinancialIndexEntry> series = BCBFinancialService.getIPCASeries("01/01/2023", "01/06/2023");
      printSeries("IPCA", series);
    } catch (RFWException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testDownloadPoupanca2012() {
    try {
      List<FinancialIndexEntry> series = BCBFinancialService.getPoupanca2012Series("01/01/2023", "01/06/2023");
      printSeries("Poupança 2012", series);
    } catch (RFWException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testDownloadSelicDaily() {
    try {
      List<FinancialIndexEntry> series = BCBFinancialService.getSelicDailySeries("01/01/2023", "01/06/2023");
      printSeries("Selic Diária", series);
    } catch (RFWException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testDownloadSelicMonthly() {
    try {
      List<FinancialIndexEntry> series = BCBFinancialService.getSelicMonthlySeries("01/01/2023", "01/06/2023");
      printSeries("Selic Mensal", series);
    } catch (RFWException e) {
      e.printStackTrace();
    }
  }

  private void printSeries(String title, List<FinancialIndexEntry> series) {
    System.out.println("=== " + title + " ===");
    for (FinancialIndexEntry entry : series) {
      System.out.println(entry.getDate() + " -> " + entry.getValue());
    }
    System.out.println();
  }
}
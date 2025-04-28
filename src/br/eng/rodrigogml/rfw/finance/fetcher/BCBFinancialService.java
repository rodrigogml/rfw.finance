package br.eng.rodrigogml.rfw.finance.fetcher;

import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import br.eng.rodrigogml.rfw.kernel.exceptions.RFWCriticalException;
import br.eng.rodrigogml.rfw.kernel.exceptions.RFWException;
import br.eng.rodrigogml.rfw.kernel.logger.RFWLogger;

/**
 * Servi�o utilit�rio para busca de �ndices financeiros diretamente da API do Banco Central do Brasil (BCB).
 *
 * Esta classe realiza o download das s�ries financeiras e converte os dados em objetos Java estruturados.
 *
 * @author Rodrigo Leit�o
 * @since (28 de abr. de 2025)
 */
public class BCBFinancialService {

  /**
   * Enumera��o dos �ndices financeiros dispon�veis para consulta no BCB.
   */
  public static enum BCBFinancialIndex {
    DOLLAR_BUY(10813),
    DOLLAR_SELL(1),
    IPCA(433),
    POUPANCA_2012(196),
    SELIC_DAILY(11),
    SELIC_MONTHLY(4390);

    private final int seriesId;

    private BCBFinancialIndex(int seriesId) {
      this.seriesId = seriesId;
    }

    public int getSeriesId() {
      return seriesId;
    }
  }

  private static final String BASE_URL = "https://api.bcb.gov.br/dados/serie/bcdata.sgs.";

  private BCBFinancialService() {
    // Classe utilit�ria: n�o deve ser instanciada.
  }

  /**
   * Realiza o download dos dados de uma s�rie financeira em formato JSON.
   *
   * @param index �ndice financeiro a ser consultado.
   * @param startDate Data inicial no formato dd/MM/yyyy.
   * @param endDate Data final no formato dd/MM/yyyy.
   * @return String com o conte�do JSON da resposta.
   * @throws RFWException Em caso de erro na conex�o ou na leitura da resposta.
   */
  public static String downloadFinancialSeries(BCBFinancialIndex index, String startDate, String endDate) throws RFWException {
    String urlString = BASE_URL + index.getSeriesId() + "/dados?formato=json&dataInicial=" + startDate + "&dataFinal=" + endDate;
    RFWLogger.logInfo("Iniciando download da s�rie: " + index.name());

    try {
      URL url = new URL(urlString);
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setRequestMethod("GET");
      connection.setConnectTimeout(10000);
      connection.setReadTimeout(10000);

      int responseCode = connection.getResponseCode();
      if (responseCode != 200) {
        throw new RFWCriticalException("RFW_000060", new String[] { index.name(), String.valueOf(responseCode) });
      }

      try (Scanner scanner = new Scanner(connection.getInputStream(), "UTF-8")) {
        scanner.useDelimiter("\\A");
        String result = scanner.hasNext() ? scanner.next() : "";
        RFWLogger.logInfo("Download conclu�do com sucesso para a s�rie: " + index.name());
        return result;
      }
    } catch (Exception e) {
      RFWLogger.logException(e);
      throw new RFWCriticalException("RFW_000061", new String[] { index.name() }, e);
    }
  }

  /**
   * Busca e retorna os dados da s�rie de compra do d�lar.
   *
   * @param startDate Data inicial no formato dd/MM/yyyy.
   * @param endDate Data final no formato dd/MM/yyyy.
   * @return Lista de entradas financeiras.
   * @throws RFWException Em caso de erro na consulta ou no processamento.
   */
  public static List<FinancialIndexEntry> getDollarBuySeries(String startDate, String endDate) throws RFWException {
    String json = downloadFinancialSeries(BCBFinancialIndex.DOLLAR_BUY, startDate, endDate);
    return parseFinancialIndexJson(json);
  }

  /**
   * Busca e retorna os dados da s�rie de venda do d�lar.
   *
   * @param startDate Data inicial no formato dd/MM/yyyy.
   * @param endDate Data final no formato dd/MM/yyyy.
   * @return Lista de entradas financeiras.
   * @throws RFWException Em caso de erro na consulta ou no processamento.
   */
  public static List<FinancialIndexEntry> getDollarSellSeries(String startDate, String endDate) throws RFWException {
    String json = downloadFinancialSeries(BCBFinancialIndex.DOLLAR_SELL, startDate, endDate);
    return parseFinancialIndexJson(json);
  }

  /**
   * Busca e retorna os dados da s�rie de IPCA (�ndice de Pre�os ao Consumidor Amplo).
   *
   * @param startDate Data inicial no formato dd/MM/yyyy.
   * @param endDate Data final no formato dd/MM/yyyy.
   * @return Lista de entradas financeiras.
   * @throws RFWException Em caso de erro na consulta ou no processamento.
   */
  public static List<FinancialIndexEntry> getIPCASeries(String startDate, String endDate) throws RFWException {
    String json = downloadFinancialSeries(BCBFinancialIndex.IPCA, startDate, endDate);
    return parseFinancialIndexJson(json);
  }

  /**
   * Busca e retorna os dados da s�rie de rendimento da poupan�a p�s-2012.
   *
   * @param startDate Data inicial no formato dd/MM/yyyy.
   * @param endDate Data final no formato dd/MM/yyyy.
   * @return Lista de entradas financeiras.
   * @throws RFWException Em caso de erro na consulta ou no processamento.
   */
  public static List<FinancialIndexEntry> getPoupanca2012Series(String startDate, String endDate) throws RFWException {
    String json = downloadFinancialSeries(BCBFinancialIndex.POUPANCA_2012, startDate, endDate);
    return parseFinancialIndexJson(json);
  }

  /**
   * Busca e retorna os dados da s�rie di�ria da taxa SELIC.
   *
   * @param startDate Data inicial no formato dd/MM/yyyy.
   * @param endDate Data final no formato dd/MM/yyyy.
   * @return Lista de entradas financeiras.
   * @throws RFWException Em caso de erro na consulta ou no processamento.
   */
  public static List<FinancialIndexEntry> getSelicDailySeries(String startDate, String endDate) throws RFWException {
    String json = downloadFinancialSeries(BCBFinancialIndex.SELIC_DAILY, startDate, endDate);
    return parseFinancialIndexJson(json);
  }

  /**
   * Busca e retorna os dados da s�rie mensal da taxa SELIC.
   *
   * @param startDate Data inicial no formato dd/MM/yyyy.
   * @param endDate Data final no formato dd/MM/yyyy.
   * @return Lista de entradas financeiras.
   * @throws RFWException Em caso de erro na consulta ou no processamento.
   */
  public static List<FinancialIndexEntry> getSelicMonthlySeries(String startDate, String endDate) throws RFWException {
    String json = downloadFinancialSeries(BCBFinancialIndex.SELIC_MONTHLY, startDate, endDate);
    return parseFinancialIndexJson(json);
  }

  /**
   * Realiza o parse do conte�do JSON retornado pela API do BCB em uma lista de entradas financeiras.
   *
   * @param json String no formato JSON contendo os dados financeiros.
   * @return Lista de FinancialIndexEntry.
   * @throws RFWException Em caso de erro na convers�o do JSON.
   */
  private static List<FinancialIndexEntry> parseFinancialIndexJson(String json) throws RFWException {
    List<FinancialIndexEntry> entries = new ArrayList<>();
    try {
      JsonArray array = JsonParser.parseString(json).getAsJsonArray();
      for (JsonElement element : array) {
        JsonObject obj = element.getAsJsonObject();
        String dataStr = obj.get("data").getAsString();
        String valorStr = obj.get("valor").getAsString();

        LocalDate date = LocalDate.parse(dataStr, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        BigDecimal value = new BigDecimal(valorStr.replace(",", "."));

        entries.add(new FinancialIndexEntry(date, value));
      }
    } catch (Exception e) {
      RFWLogger.logException(e);
      throw new RFWCriticalException("RFW_000059", e);
    }
    return entries;
  }

}

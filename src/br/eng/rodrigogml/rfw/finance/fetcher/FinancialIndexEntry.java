package br.eng.rodrigogml.rfw.finance.fetcher;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Representa um registro de índice financeiro do Banco Central do Brasil.
 *
 * @author Rodrigo Leitão
 * @since (28 de abr. de 2025)
 */
public class FinancialIndexEntry {

  /**
   * Data da Série de Dadis
   */
  private LocalDate date;

  /**
   * Valor do elemento da série
   */
  private BigDecimal value;

  /**
   * Construtor padrão da classe.
   *
   * @param date Data do registro.
   * @param value Valor do índice na data.
   */
  public FinancialIndexEntry(LocalDate date, BigDecimal value) {
    this.date = date;
    this.value = value;
  }

  /**
   * # data da Série de Dadis.
   *
   * @return the data da Série de Dadis
   */
  public LocalDate getDate() {
    return date;
  }

  /**
   * # data da Série de Dadis.
   *
   * @param date the new data da Série de Dadis
   */
  public void setDate(LocalDate date) {
    this.date = date;
  }

  /**
   * # valor do elemento da série.
   *
   * @return the valor do elemento da série
   */
  public BigDecimal getValue() {
    return value;
  }

  /**
   * # valor do elemento da série.
   *
   * @param value the new valor do elemento da série
   */
  public void setValue(BigDecimal value) {
    this.value = value;
  }
}

package br.eng.rodrigogml.rfw.finance.fetcher;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Representa um registro de �ndice financeiro do Banco Central do Brasil.
 *
 * @author Rodrigo Leit�o
 * @since (28 de abr. de 2025)
 */
public class FinancialIndexEntry {

  /**
   * Data da S�rie de Dadis
   */
  private LocalDate date;

  /**
   * Valor do elemento da s�rie
   */
  private BigDecimal value;

  /**
   * Construtor padr�o da classe.
   *
   * @param date Data do registro.
   * @param value Valor do �ndice na data.
   */
  public FinancialIndexEntry(LocalDate date, BigDecimal value) {
    this.date = date;
    this.value = value;
  }

  /**
   * # data da S�rie de Dadis.
   *
   * @return the data da S�rie de Dadis
   */
  public LocalDate getDate() {
    return date;
  }

  /**
   * # data da S�rie de Dadis.
   *
   * @param date the new data da S�rie de Dadis
   */
  public void setDate(LocalDate date) {
    this.date = date;
  }

  /**
   * # valor do elemento da s�rie.
   *
   * @return the valor do elemento da s�rie
   */
  public BigDecimal getValue() {
    return value;
  }

  /**
   * # valor do elemento da s�rie.
   *
   * @param value the new valor do elemento da s�rie
   */
  public void setValue(BigDecimal value) {
    this.value = value;
  }
}

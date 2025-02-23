package br.eng.rodrigogml.rfw.finance.cnab240.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import br.eng.rodrigogml.rfw.finance.cnab240.parser.data.CNAB240Lote;
import br.eng.rodrigogml.rfw.finance.cnab240.parser.data.CNAB240RegisterA;
import br.eng.rodrigogml.rfw.finance.cnab240.parser.data.CNAB240RegisterDetail;
import br.eng.rodrigogml.rfw.finance.cnab240.parser.data.CNAB240RegisterJ;
import br.eng.rodrigogml.rfw.kernel.exceptions.RFWCriticalException;
import br.eng.rodrigogml.rfw.kernel.exceptions.RFWException;
import br.eng.rodrigogml.rfw.kernel.exceptions.RFWValidationException;
import br.eng.rodrigogml.rfw.kernel.logger.RFWLogger;

/**
 * Description: Esta classe tem a finalidade de converter um arquivo de retorno CNAB240 recebido de outro sistema, valid�-lo e transform�-lo em um objeto estruturado para simplificar sua interpreta��o pelo sistema.<br>
 *
 * @author Rodrigo Leit�o
 * @since (20 de fev. de 2025)
 */
public class CNAB240Parser {

  /**
   * G018 N�mero Seq�encial do Arquivo<br>
   * N�mero seq�encial adotado e controlado pelo respons�vel pela gera��o do arquivo para ordenar a disposi��o dos arquivos encaminhados. <br>
   * Evoluir um n�mero seq�encial a cada header de arquivo.
   */
  private String numeroSequencialArquivo;

  /**
   * G001 C�digo do Banco na Compensa��o<br>
   * C�digo fornecido pelo Banco Central para identifica��o do Banco que est� recebendo ou enviando o arquivo, com o qual se firmou o contrato de presta��o de servi�os. <br>
   * Preencher com �988� quando a transfer�ncia for efetuada para outra institui��o financeira utilizando o c�digo ISPB. Neste caso, dever� ser preenchido o c�digo ISPB no campo 26.3B.
   */
  private String codigoBanco = null;

  /**
   * G005 Tipo de Inscri��o da Empresa <br>
   * C�digo que identifica o tipo de inscri��o da Empresa ou Pessoa F�sica perante uma Institui��o governamental.Dom�nio:
   * <ul>
   * <li>'0' = Isento / N�o Informado
   * <li>'1' = CPF
   * <li>'2' = CGC / CNPJ
   * <li>'3' = PIS / PASEP
   * <li>'9' = Outros
   * </ul>
   * <li>Preenchimento deste campo � obrigat�rio para DOC e TED (Forma de Lan�amento = 03, 41, 43).
   * <li>Para pagamento para o SIAPE com cr�dito em conta, o CPF dever� ser do 1� titular
   * <li>Para o Produto/Servi�o Cobran�a considerar como obrigat�rio, a partir de 01.06.2015, somente o CPF (c�digo 1) ou o CNPJ (c�digo 2). Os demais c�digos n�o dever�o ser utilizados.
   */
  private String tipoInscricao = null;

  /**
   * G022 Para Uso Reservado da Empresa<br>
   * Texto de observa��es destinado para uso exclusivo da Empresa.
   */
  private String reservadoEmpresa;

  /**
   * G006 N�mero de Inscri��o da Empresa<br>
   * Deve ser preenchido de acordo com o tipo de inscri��o definido no campo {@link #tipoInscricao}.<br>
   * N�mero de inscri��o da Empresa ou Pessoa F�sica perante uma Institui��o governamental. <br>
   * Quando o Tipo de Inscri��o for igual a zero (n�o informado), preencher com zeros.
   */
  private String numeroInscricao;

  /**
   * G007 C�digo do Conv�nio no Banco<br>
   * C�digo adotado pelo Banco para identificar o Contrato entre este e a Empresa Cliente. G <Br>
   */
  private String codigoConvenio;

  /**
   * G008 Ag�ncia Mantenedora da Conta<br>
   * C�digo adotado pelo Banco respons�vel pela conta, para identificar a qual unidade est� vinculada a conta corrente.<Br>
   * Ref�re-se � ag�ncia da conta que receber� o arquivo de lote.
   */
  private String agencia;

  /**
   * G009 D�gito Verificador da Ag�ncia<Br>
   * C�digo adotado pelo Banco respons�vel pela conta corrente, para verifica��o da autenticidade do C�digo da Ag�ncia.<br>
   * N�o definir se o banco n�o utilizar DV para a ag�ncia.
   *
   */
  private String agenciaDV;

  /**
   * G010 N�mero da Conta Corrente<br>
   * N�mero adotado pelo Banco, para identificar univocamente a conta corrente utilizada pelo Cliente.
   */
  private String contaNumero;

  /**
   * G011 D�gito Verificador da Conta<br>
   * C�digo adotado pelo respons�vel pela conta corrente, para verifica��o da autenticidade do N�mero da Conta Corrente.<br>
   * Para os Bancos que se utilizam de duas posi��es para o D�gito Verificador do N�mero da Conta Corrente, preencher este campo com a 1� posi��o deste d�gito.<br>
   * Exemplo :
   * <ul>
   * <li>N�mero C/C = 45981-36
   * <li>Neste caso: D�gito Verificador da Conta = 3
   * </ul>
   * <b>ATEN��O:</B>A maioria doa bancos n�o utiliza esse DV de conta separado, mas sim o DV do conjunto Ag�ncia + Conta, que deve ser definido no {@link #agenciaContaDV}.
   */
  private String contaDV;

  /**
   * G012 D�gito Verificador da Ag�ncia / Conta Corrente<br>
   * C�digo adotado pelo Banco respons�vel pela conta corrente, para verifica��o da autenticidade do par C�digo da Ag�ncia / N�mero da Conta Corrente.<br>
   * Para os Bancos que se utilizam de duas posi��es para o D�gito Verificador do N�mero da Conta Corrente, preencher este campo com a 2� posi��o deste d�gito.<br>
   * Exemplo:
   * <ul>
   * <li>N�mero C/C = 45981-36
   * <li>Neste caso: D�gito Verificador da Ag/Conta = 6
   * </ul>
   */
  private String agenciaContaDV;

  /**
   * Vers�o do Layout do Header do Arquivo
   */
  private String layout;

  /**
   * Cole��o com os lotes encontrados dentro do arquivo
   */
  private List<CNAB240Lote> lotes = new ArrayList<CNAB240Lote>();

  /**
   * G015 C�digo Remessa / Retorno<br>
   * C�digo adotado pela FEBRABAN para qualificar o envio ou devolu��o de arquivo entre a Empresa Cliente e o Banco prestador dos Servi�os. <br>
   * Dom�nio:
   * <li>'1' = Remessa (Cliente > Banco)
   * <li>'2' = Retorno (Banco > Cliente)
   */
  private String remessaRetorno;

  // Mant�m a refer�ncia para o �ltimo lote criado
  private CNAB240Lote lastLote = null;

  // Mant�m a refer�ncia para o �ltimo registro de retalhe criado.
  private CNAB240RegisterDetail lastRegister = null;

  /**
   * Instancia um objeto que representa um arquivo de retorno CNAB240.
   *
   * @param retFile InputStream com o conte�do do arquivo.
   * @throws RFWException
   */
  public CNAB240Parser(InputStream retFile) throws RFWException {
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(retFile))) {
      String line;
      int countLine = 0;
      int lastRegisterType = -1; // Marca o �ltimo registro apra saber se o registro atual � esperado

      while ((line = reader.readLine()) != null) {
        countLine++;
        if ("".equals(line)) continue;

        if (line.length() != 240) throw new RFWValidationException("A linha '${0}' n�o cont�m 240 caracteres!", new String[] { "" + countLine });

        String tipoRegistro = line.substring(7, 8);

        // Separa a linha de acordo com o tipo de registro:
        switch (tipoRegistro) {
          case "0": // Header do Arquivo
            if (lastRegisterType != -1) {
              throw new RFWValidationException("O registro do tipo '0' � esperado na primeira linha do arquivo. Encontrado na linha '${0}'.", new String[] { "" + countLine });
            }
            processFileHeader(line, countLine);
            break;
          case "1": // Header do Lote
            if (lastRegisterType != 0 && lastRegisterType != 5) {
              throw new RFWValidationException("O registro do tipo '1' � esperado ap�s a abertura do arquivo ou ap�s o fechamento de outro lote. Encontrado na linha '${0}'.", new String[] { "" + countLine });
            }
            lastLote = new CNAB240Lote(line);
            this.lotes.add(lastLote);
            break;
          case "3": // Detalhe
            if (lastRegisterType != 3 && lastRegisterType != 1) {
              throw new RFWValidationException("O registro do tipo '3' � esperado ap�s a abertura do lote ou ap�s outro registro detalhe. Encontrado na linha '${0}'.", new String[] { "" + countLine });
            }
            String segmento = line.substring(13, 14);
            switch (segmento) {
              case "J":
                if (" ".equals(line.substring(14, 15)) && "52".equals(line.substring(17, 19))) { // Utiliza o Branco obrigat�rio do registro 52, que n�o � branco no registro J principal, para idenficicar se � um registro J ou o complementar J-52
                  if (lastRegister == null || !(lastRegister instanceof CNAB240RegisterJ)) {
                    throw new RFWCriticalException("Encontrado um registro detalhe J-52 sem um registro J predecessor na linha '${0}'.", new String[] { "" + countLine });
                  }
                  ((CNAB240RegisterJ) lastRegister).addLine(line);
                  lastRegister = null; // N�o permite outras adi��es de linha SubRegistros no mesmo J
                } else {
                  lastRegister = new CNAB240RegisterJ(line);
                  lastLote.getRegisterList().add(lastRegister);
                }
                break;
              case "A":
                lastRegister = new CNAB240RegisterA(line);
                lastLote.getRegisterList().add(lastRegister);
                break;
              default:
                RFWLogger.logDebug("Registro Segmento '${0}' ignorado por n�o haver suporte no parser!", new String[] { segmento });
                break;
            }
            break;
          case "5": // Trailer do Lote
            if (lastRegisterType != 3) {
              throw new RFWValidationException("O registro do tipo '5' � esperado ap�s o lan�amento de registros detalhes. Encontrado na linha '${0}'.", new String[] { "" + countLine });
            }
            this.lastLote = null;
            this.lastRegister = null;
            break;
          case "9": // Trailer do Arquivo
            if (lastRegisterType != 5) {
              throw new RFWValidationException("O registro do tipo '9' � esperado ap�s o fechamento dos lotes. Encontrado na linha '${0}'.", new String[] { "" + countLine });
            }
            this.lastLote = null;
            break;
          default:
            throw new RFWValidationException("Tipo de registro '${0}' desconhecido encontrado na linha'${1}'.", new String[] { tipoRegistro, "" + countLine });
        }
        lastRegisterType = Integer.parseInt(tipoRegistro);
      }
    } catch (IOException e) {
      throw new RFWCriticalException("Falha ao l�r conte�do do arquivo!", e);
    }
  }

  /**
   * Processa o registro de header de arquivo
   *
   * @param line
   * @param countLine
   * @throws RFWCriticalException
   */
  private void processFileHeader(String line, int countLine) throws RFWCriticalException {
    // No da Vers�o do Layout do Arquivo 164-166 3 - Num
    this.layout = line.substring(163, 166);
    if (this.layout.equals("103")) {
      // C�digo do Banco na Compensa��o 1-3 3 - Num
      this.codigoBanco = line.substring(0, 3);
      // Tipo de Inscri��o da Empresa 18-18 1 - Num
      this.tipoInscricao = line.substring(17, 18);
      // N�mero de Inscri��o da Empresa 19-32 14 - Num
      this.numeroInscricao = line.substring(18, 32);
      // Ag�ncia Mantenedora da Conta 53-57 5 - Num
      this.agencia = line.substring(52, 57);
      // D�gito Verificador da Ag�ncia 58-58 1 - Alfa
      this.agenciaDV = line.substring(57, 58);
      // N�mero da Conta Corrente 59-70 12 - Num
      this.contaNumero = line.substring(58, 70);
      // D�gito Verificador da Conta 71-71 1 - Alfa
      this.contaDV = line.substring(70, 71);
      // D�gito Verificador da Ag/Conta 72-72 1 - Alfa
      this.agenciaContaDV = line.substring(71, 72);
      // C�digo Remessa / Retorno 143 143 1 - Num
      this.remessaRetorno = line.substring(142, 143);
      // N�mero Seq�encial do Arquivo 158 163 6 - Num
      this.numeroSequencialArquivo = line.substring(157, 163);
      // Para Uso Reservado da Empresa 192 211 20 - Alfa
      this.reservadoEmpresa = line.substring(191, 211);
    } else {
      throw new RFWCriticalException("Layout de Header de Lote desconhecido: '${0}'.", new String[] { this.layout });
    }
  }

  /**
   * # g001 C�digo do Banco na Compensa��o<br>
   * C�digo fornecido pelo Banco Central para identifica��o do Banco que est� recebendo ou enviando o arquivo, com o qual se firmou o contrato de presta��o de servi�os. <br>
   * Preencher com �988� quando a transfer�ncia for efetuada para outra institui��o financeira utilizando o c�digo ISPB. Neste caso, dever� ser preenchido o c�digo ISPB no campo 26.3B.
   *
   * @return the g001 C�digo do Banco na Compensa��o<br>
   *         C�digo fornecido pelo Banco Central para identifica��o do Banco que est� recebendo ou enviando o arquivo, com o qual se firmou o contrato de presta��o de servi�os
   */
  public String getCodigoBanco() {
    return codigoBanco;
  }

  /**
   * # g001 C�digo do Banco na Compensa��o<br>
   * C�digo fornecido pelo Banco Central para identifica��o do Banco que est� recebendo ou enviando o arquivo, com o qual se firmou o contrato de presta��o de servi�os. <br>
   * Preencher com �988� quando a transfer�ncia for efetuada para outra institui��o financeira utilizando o c�digo ISPB. Neste caso, dever� ser preenchido o c�digo ISPB no campo 26.3B.
   *
   * @param codigoBanco the new g001 C�digo do Banco na Compensa��o<br>
   *          C�digo fornecido pelo Banco Central para identifica��o do Banco que est� recebendo ou enviando o arquivo, com o qual se firmou o contrato de presta��o de servi�os
   */
  public void setCodigoBanco(String codigoBanco) {
    this.codigoBanco = codigoBanco;
  }

  /**
   * # g005 Tipo de Inscri��o da Empresa <br>
   * C�digo que identifica o tipo de inscri��o da Empresa ou Pessoa F�sica perante uma Institui��o governamental.Dom�nio:
   * <ul>
   * <li>'0' = Isento / N�o Informado
   * <li>'1' = CPF
   * <li>'2' = CGC / CNPJ
   * <li>'3' = PIS / PASEP
   * <li>'9' = Outros
   * </ul>
   * <li>Preenchimento deste campo � obrigat�rio para DOC e TED (Forma de Lan�amento = 03, 41, 43).
   * <li>Para pagamento para o SIAPE com cr�dito em conta, o CPF dever� ser do 1� titular
   * <li>Para o Produto/Servi�o Cobran�a considerar como obrigat�rio, a partir de 01.06.2015, somente o CPF (c�digo 1) ou o CNPJ (c�digo 2). Os demais c�digos n�o dever�o ser utilizados.
   *
   * @return the g005 Tipo de Inscri��o da Empresa <br>
   *         C�digo que identifica o tipo de inscri��o da Empresa ou Pessoa F�sica perante uma Institui��o governamental
   */
  public String getTipoInscricao() {
    return tipoInscricao;
  }

  /**
   * # g005 Tipo de Inscri��o da Empresa <br>
   * C�digo que identifica o tipo de inscri��o da Empresa ou Pessoa F�sica perante uma Institui��o governamental.Dom�nio:
   * <ul>
   * <li>'0' = Isento / N�o Informado
   * <li>'1' = CPF
   * <li>'2' = CGC / CNPJ
   * <li>'3' = PIS / PASEP
   * <li>'9' = Outros
   * </ul>
   * <li>Preenchimento deste campo � obrigat�rio para DOC e TED (Forma de Lan�amento = 03, 41, 43).
   * <li>Para pagamento para o SIAPE com cr�dito em conta, o CPF dever� ser do 1� titular
   * <li>Para o Produto/Servi�o Cobran�a considerar como obrigat�rio, a partir de 01.06.2015, somente o CPF (c�digo 1) ou o CNPJ (c�digo 2). Os demais c�digos n�o dever�o ser utilizados.
   *
   * @param tipoInscricao the new g005 Tipo de Inscri��o da Empresa <br>
   *          C�digo que identifica o tipo de inscri��o da Empresa ou Pessoa F�sica perante uma Institui��o governamental
   */
  public void setTipoInscricao(String tipoInscricao) {
    this.tipoInscricao = tipoInscricao;
  }

  /**
   * # g006 N�mero de Inscri��o da Empresa<br>
   * Deve ser preenchido de acordo com o tipo de inscri��o definido no campo {@link #tipoInscricao}.<br>
   * N�mero de inscri��o da Empresa ou Pessoa F�sica perante uma Institui��o governamental. <br>
   * Quando o Tipo de Inscri��o for igual a zero (n�o informado), preencher com zeros.
   *
   * @return the g006 N�mero de Inscri��o da Empresa<br>
   *         Deve ser preenchido de acordo com o tipo de inscri��o definido no campo {@link #tipoInscricao}
   */
  public String getNumeroInscricao() {
    return numeroInscricao;
  }

  /**
   * # g006 N�mero de Inscri��o da Empresa<br>
   * Deve ser preenchido de acordo com o tipo de inscri��o definido no campo {@link #tipoInscricao}.<br>
   * N�mero de inscri��o da Empresa ou Pessoa F�sica perante uma Institui��o governamental. <br>
   * Quando o Tipo de Inscri��o for igual a zero (n�o informado), preencher com zeros.
   *
   * @param numeroInscricao the new g006 N�mero de Inscri��o da Empresa<br>
   *          Deve ser preenchido de acordo com o tipo de inscri��o definido no campo {@link #tipoInscricao}
   */
  public void setNumeroInscricao(String numeroInscricao) {
    this.numeroInscricao = numeroInscricao;
  }

  /**
   * # g007 C�digo do Conv�nio no Banco<br>
   * C�digo adotado pelo Banco para identificar o Contrato entre este e a Empresa Cliente. G <Br>
   * .
   *
   * @return the g007 C�digo do Conv�nio no Banco<br>
   *         C�digo adotado pelo Banco para identificar o Contrato entre este e a Empresa Cliente
   */
  public String getCodigoConvenio() {
    return codigoConvenio;
  }

  /**
   * # g007 C�digo do Conv�nio no Banco<br>
   * C�digo adotado pelo Banco para identificar o Contrato entre este e a Empresa Cliente. G <Br>
   * .
   *
   * @param codigoConvenio the new g007 C�digo do Conv�nio no Banco<br>
   *          C�digo adotado pelo Banco para identificar o Contrato entre este e a Empresa Cliente
   */
  public void setCodigoConvenio(String codigoConvenio) {
    this.codigoConvenio = codigoConvenio;
  }

  /**
   * # g008 Ag�ncia Mantenedora da Conta<br>
   * C�digo adotado pelo Banco respons�vel pela conta, para identificar a qual unidade est� vinculada a conta corrente.<Br>
   * Ref�re-se � ag�ncia da conta que receber� o arquivo de lote.
   *
   * @return the g008 Ag�ncia Mantenedora da Conta<br>
   *         C�digo adotado pelo Banco respons�vel pela conta, para identificar a qual unidade est� vinculada a conta corrente
   */
  public String getAgencia() {
    return agencia;
  }

  /**
   * # g008 Ag�ncia Mantenedora da Conta<br>
   * C�digo adotado pelo Banco respons�vel pela conta, para identificar a qual unidade est� vinculada a conta corrente.<Br>
   * Ref�re-se � ag�ncia da conta que receber� o arquivo de lote.
   *
   * @param agencia the new g008 Ag�ncia Mantenedora da Conta<br>
   *          C�digo adotado pelo Banco respons�vel pela conta, para identificar a qual unidade est� vinculada a conta corrente
   */
  public void setAgencia(String agencia) {
    this.agencia = agencia;
  }

  /**
   * # g009 D�gito Verificador da Ag�ncia<Br>
   * C�digo adotado pelo Banco respons�vel pela conta corrente, para verifica��o da autenticidade do C�digo da Ag�ncia.<br>
   * N�o definir se o banco n�o utilizar DV para a ag�ncia.
   *
   * @return the g009 D�gito Verificador da Ag�ncia<Br>
   *         C�digo adotado pelo Banco respons�vel pela conta corrente, para verifica��o da autenticidade do C�digo da Ag�ncia
   */
  public String getAgenciaDV() {
    return agenciaDV;
  }

  /**
   * # g009 D�gito Verificador da Ag�ncia<Br>
   * C�digo adotado pelo Banco respons�vel pela conta corrente, para verifica��o da autenticidade do C�digo da Ag�ncia.<br>
   * N�o definir se o banco n�o utilizar DV para a ag�ncia.
   *
   * @param agenciaDV the new g009 D�gito Verificador da Ag�ncia<Br>
   *          C�digo adotado pelo Banco respons�vel pela conta corrente, para verifica��o da autenticidade do C�digo da Ag�ncia
   */
  public void setAgenciaDV(String agenciaDV) {
    this.agenciaDV = agenciaDV;
  }

  /**
   * # g010 N�mero da Conta Corrente<br>
   * N�mero adotado pelo Banco, para identificar univocamente a conta corrente utilizada pelo Cliente.
   *
   * @return the g010 N�mero da Conta Corrente<br>
   *         N�mero adotado pelo Banco, para identificar univocamente a conta corrente utilizada pelo Cliente
   */
  public String getContaNumero() {
    return contaNumero;
  }

  /**
   * # g010 N�mero da Conta Corrente<br>
   * N�mero adotado pelo Banco, para identificar univocamente a conta corrente utilizada pelo Cliente.
   *
   * @param contaNumero the new g010 N�mero da Conta Corrente<br>
   *          N�mero adotado pelo Banco, para identificar univocamente a conta corrente utilizada pelo Cliente
   */
  public void setContaNumero(String contaNumero) {
    this.contaNumero = contaNumero;
  }

  /**
   * # g011 D�gito Verificador da Conta<br>
   * C�digo adotado pelo respons�vel pela conta corrente, para verifica��o da autenticidade do N�mero da Conta Corrente.<br>
   * Para os Bancos que se utilizam de duas posi��es para o D�gito Verificador do N�mero da Conta Corrente, preencher este campo com a 1� posi��o deste d�gito.<br>
   * Exemplo :
   * <ul>
   * <li>N�mero C/C = 45981-36
   * <li>Neste caso: D�gito Verificador da Conta = 3
   * </ul>
   * <b>ATEN��O:</B>A maioria doa bancos n�o utiliza esse DV de conta separado, mas sim o DV do conjunto Ag�ncia + Conta, que deve ser definido no {@link #agenciaContaDV}.
   *
   * @return the g011 D�gito Verificador da Conta<br>
   *         C�digo adotado pelo respons�vel pela conta corrente, para verifica��o da autenticidade do N�mero da Conta Corrente
   */
  public String getContaDV() {
    return contaDV;
  }

  /**
   * # g011 D�gito Verificador da Conta<br>
   * C�digo adotado pelo respons�vel pela conta corrente, para verifica��o da autenticidade do N�mero da Conta Corrente.<br>
   * Para os Bancos que se utilizam de duas posi��es para o D�gito Verificador do N�mero da Conta Corrente, preencher este campo com a 1� posi��o deste d�gito.<br>
   * Exemplo :
   * <ul>
   * <li>N�mero C/C = 45981-36
   * <li>Neste caso: D�gito Verificador da Conta = 3
   * </ul>
   * <b>ATEN��O:</B>A maioria doa bancos n�o utiliza esse DV de conta separado, mas sim o DV do conjunto Ag�ncia + Conta, que deve ser definido no {@link #agenciaContaDV}.
   *
   * @param contaDV the new g011 D�gito Verificador da Conta<br>
   *          C�digo adotado pelo respons�vel pela conta corrente, para verifica��o da autenticidade do N�mero da Conta Corrente
   */
  public void setContaDV(String contaDV) {
    this.contaDV = contaDV;
  }

  /**
   * # g012 D�gito Verificador da Ag�ncia / Conta Corrente<br>
   * C�digo adotado pelo Banco respons�vel pela conta corrente, para verifica��o da autenticidade do par C�digo da Ag�ncia / N�mero da Conta Corrente.<br>
   * Para os Bancos que se utilizam de duas posi��es para o D�gito Verificador do N�mero da Conta Corrente, preencher este campo com a 2� posi��o deste d�gito.<br>
   * Exemplo:
   * <ul>
   * <li>N�mero C/C = 45981-36
   * <li>Neste caso: D�gito Verificador da Ag/Conta = 6
   * </ul>
   * .
   *
   * @return the g012 D�gito Verificador da Ag�ncia / Conta Corrente<br>
   *         C�digo adotado pelo Banco respons�vel pela conta corrente, para verifica��o da autenticidade do par C�digo da Ag�ncia / N�mero da Conta Corrente
   */
  public String getAgenciaContaDV() {
    return agenciaContaDV;
  }

  /**
   * # g012 D�gito Verificador da Ag�ncia / Conta Corrente<br>
   * C�digo adotado pelo Banco respons�vel pela conta corrente, para verifica��o da autenticidade do par C�digo da Ag�ncia / N�mero da Conta Corrente.<br>
   * Para os Bancos que se utilizam de duas posi��es para o D�gito Verificador do N�mero da Conta Corrente, preencher este campo com a 2� posi��o deste d�gito.<br>
   * Exemplo:
   * <ul>
   * <li>N�mero C/C = 45981-36
   * <li>Neste caso: D�gito Verificador da Ag/Conta = 6
   * </ul>
   * .
   *
   * @param agenciaContaDV the new g012 D�gito Verificador da Ag�ncia / Conta Corrente<br>
   *          C�digo adotado pelo Banco respons�vel pela conta corrente, para verifica��o da autenticidade do par C�digo da Ag�ncia / N�mero da Conta Corrente
   */
  public void setAgenciaContaDV(String agenciaContaDV) {
    this.agenciaContaDV = agenciaContaDV;
  }

  /**
   * # g018 N�mero Seq�encial do Arquivo<br>
   * N�mero seq�encial adotado e controlado pelo respons�vel pela gera��o do arquivo para ordenar a disposi��o dos arquivos encaminhados. <br>
   * Evoluir um n�mero seq�encial a cada header de arquivo.
   *
   * @return the g018 N�mero Seq�encial do Arquivo<br>
   *         N�mero seq�encial adotado e controlado pelo respons�vel pela gera��o do arquivo para ordenar a disposi��o dos arquivos encaminhados
   */
  public String getNumeroSequencialArquivo() {
    return numeroSequencialArquivo;
  }

  /**
   * # g018 N�mero Seq�encial do Arquivo<br>
   * N�mero seq�encial adotado e controlado pelo respons�vel pela gera��o do arquivo para ordenar a disposi��o dos arquivos encaminhados. <br>
   * Evoluir um n�mero seq�encial a cada header de arquivo.
   *
   * @param numeroSequencialArquivo the new g018 N�mero Seq�encial do Arquivo<br>
   *          N�mero seq�encial adotado e controlado pelo respons�vel pela gera��o do arquivo para ordenar a disposi��o dos arquivos encaminhados
   */
  public void setNumeroSequencialArquivo(String numeroSequencialArquivo) {
    this.numeroSequencialArquivo = numeroSequencialArquivo;
  }

  /**
   * # g022 Para Uso Reservado da Empresa<br>
   * Texto de observa��es destinado para uso exclusivo da Empresa.
   *
   * @return the g022 Para Uso Reservado da Empresa<br>
   *         Texto de observa��es destinado para uso exclusivo da Empresa
   */
  public String getReservadoEmpresa() {
    return reservadoEmpresa;
  }

  /**
   * # g022 Para Uso Reservado da Empresa<br>
   * Texto de observa��es destinado para uso exclusivo da Empresa.
   *
   * @param reservadoEmpresa the new g022 Para Uso Reservado da Empresa<br>
   *          Texto de observa��es destinado para uso exclusivo da Empresa
   */
  public void setReservadoEmpresa(String reservadoEmpresa) {
    this.reservadoEmpresa = reservadoEmpresa;
  }

  /**
   * # vers�o do Layout do Header do Arquivo.
   *
   * @return the vers�o do Layout do Header do Arquivo
   */
  public String getLayout() {
    return layout;
  }

  /**
   * # vers�o do Layout do Header do Arquivo.
   *
   * @param layout the new vers�o do Layout do Header do Arquivo
   */
  public void setLayout(String layout) {
    this.layout = layout;
  }

  /**
   * # cole��o com os lotes encontrados dentro do arquivo.
   *
   * @return the cole��o com os lotes encontrados dentro do arquivo
   */
  public List<CNAB240Lote> getLotes() {
    return lotes;
  }

  /**
   * # cole��o com os lotes encontrados dentro do arquivo.
   *
   * @param lotes the new cole��o com os lotes encontrados dentro do arquivo
   */
  public void setLotes(List<CNAB240Lote> lotes) {
    this.lotes = lotes;
  }

  /**
   * # g015 C�digo Remessa / Retorno<br>
   * C�digo adotado pela FEBRABAN para qualificar o envio ou devolu��o de arquivo entre a Empresa Cliente e o Banco prestador dos Servi�os. <br>
   * Dom�nio:
   * <li>'1' = Remessa (Cliente > Banco)
   * <li>'2' = Retorno (Banco > Cliente).
   *
   * @return the g015 C�digo Remessa / Retorno<br>
   *         C�digo adotado pela FEBRABAN para qualificar o envio ou devolu��o de arquivo entre a Empresa Cliente e o Banco prestador dos Servi�os
   */
  public String getRemessaRetorno() {
    return remessaRetorno;
  }

  /**
   * # g015 C�digo Remessa / Retorno<br>
   * C�digo adotado pela FEBRABAN para qualificar o envio ou devolu��o de arquivo entre a Empresa Cliente e o Banco prestador dos Servi�os. <br>
   * Dom�nio:
   * <li>'1' = Remessa (Cliente > Banco)
   * <li>'2' = Retorno (Banco > Cliente).
   *
   * @param remessaRetorno the new g015 C�digo Remessa / Retorno<br>
   *          C�digo adotado pela FEBRABAN para qualificar o envio ou devolu��o de arquivo entre a Empresa Cliente e o Banco prestador dos Servi�os
   */
  public void setRemessaRetorno(String remessaRetorno) {
    this.remessaRetorno = remessaRetorno;
  }

  /**
   * Transforma o resultado do m�todo {@link #getRemessaRetorno()} em booleano de acordo com a opera��o de Remessa.
   *
   * @return true se for um arquivo de remessa, false se for um arquivo de retorno, null se o m�toro original retornar null ou tiver um valor desconhecido.
   */
  public Boolean getIsRemessa() {
    if ("1".equals(getRemessaRetorno())) {
      return true;
    } else if ("2".equals(getRemessaRetorno())) {
      return false;
    }
    return null;
  }

}

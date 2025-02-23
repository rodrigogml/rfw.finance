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
 * Description: Esta classe tem a finalidade de converter um arquivo de retorno CNAB240 recebido de outro sistema, validá-lo e transformá-lo em um objeto estruturado para simplificar sua interpretação pelo sistema.<br>
 *
 * @author Rodrigo Leitão
 * @since (20 de fev. de 2025)
 */
public class CNAB240Parser {

  /**
   * G018 Número Seqüencial do Arquivo<br>
   * Número seqüencial adotado e controlado pelo responsável pela geração do arquivo para ordenar a disposição dos arquivos encaminhados. <br>
   * Evoluir um número seqüencial a cada header de arquivo.
   */
  private String numeroSequencialArquivo;

  /**
   * G001 Código do Banco na Compensação<br>
   * Código fornecido pelo Banco Central para identificação do Banco que está recebendo ou enviando o arquivo, com o qual se firmou o contrato de prestação de serviços. <br>
   * Preencher com “988” quando a transferência for efetuada para outra instituição financeira utilizando o código ISPB. Neste caso, deverá ser preenchido o código ISPB no campo 26.3B.
   */
  private String codigoBanco = null;

  /**
   * G005 Tipo de Inscrição da Empresa <br>
   * Código que identifica o tipo de inscrição da Empresa ou Pessoa Física perante uma Instituição governamental.Domínio:
   * <ul>
   * <li>'0' = Isento / Não Informado
   * <li>'1' = CPF
   * <li>'2' = CGC / CNPJ
   * <li>'3' = PIS / PASEP
   * <li>'9' = Outros
   * </ul>
   * <li>Preenchimento deste campo é obrigatório para DOC e TED (Forma de Lançamento = 03, 41, 43).
   * <li>Para pagamento para o SIAPE com crédito em conta, o CPF deverá ser do 1º titular
   * <li>Para o Produto/Serviço Cobrança considerar como obrigatório, a partir de 01.06.2015, somente o CPF (código 1) ou o CNPJ (código 2). Os demais códigos não deverão ser utilizados.
   */
  private String tipoInscricao = null;

  /**
   * G022 Para Uso Reservado da Empresa<br>
   * Texto de observações destinado para uso exclusivo da Empresa.
   */
  private String reservadoEmpresa;

  /**
   * G006 Número de Inscrição da Empresa<br>
   * Deve ser preenchido de acordo com o tipo de inscrição definido no campo {@link #tipoInscricao}.<br>
   * Número de inscrição da Empresa ou Pessoa Física perante uma Instituição governamental. <br>
   * Quando o Tipo de Inscrição for igual a zero (não informado), preencher com zeros.
   */
  private String numeroInscricao;

  /**
   * G007 Código do Convênio no Banco<br>
   * Código adotado pelo Banco para identificar o Contrato entre este e a Empresa Cliente. G <Br>
   */
  private String codigoConvenio;

  /**
   * G008 Agência Mantenedora da Conta<br>
   * Código adotado pelo Banco responsável pela conta, para identificar a qual unidade está vinculada a conta corrente.<Br>
   * Refêre-se à agência da conta que receberá o arquivo de lote.
   */
  private String agencia;

  /**
   * G009 Dígito Verificador da Agência<Br>
   * Código adotado pelo Banco responsável pela conta corrente, para verificação da autenticidade do Código da Agência.<br>
   * Não definir se o banco não utilizar DV para a agência.
   *
   */
  private String agenciaDV;

  /**
   * G010 Número da Conta Corrente<br>
   * Número adotado pelo Banco, para identificar univocamente a conta corrente utilizada pelo Cliente.
   */
  private String contaNumero;

  /**
   * G011 Dígito Verificador da Conta<br>
   * Código adotado pelo responsável pela conta corrente, para verificação da autenticidade do Número da Conta Corrente.<br>
   * Para os Bancos que se utilizam de duas posições para o Dígito Verificador do Número da Conta Corrente, preencher este campo com a 1ª posição deste dígito.<br>
   * Exemplo :
   * <ul>
   * <li>Número C/C = 45981-36
   * <li>Neste caso: Dígito Verificador da Conta = 3
   * </ul>
   * <b>ATENÇÃO:</B>A maioria doa bancos não utiliza esse DV de conta separado, mas sim o DV do conjunto Agência + Conta, que deve ser definido no {@link #agenciaContaDV}.
   */
  private String contaDV;

  /**
   * G012 Dígito Verificador da Agência / Conta Corrente<br>
   * Código adotado pelo Banco responsável pela conta corrente, para verificação da autenticidade do par Código da Agência / Número da Conta Corrente.<br>
   * Para os Bancos que se utilizam de duas posições para o Dígito Verificador do Número da Conta Corrente, preencher este campo com a 2ª posição deste dígito.<br>
   * Exemplo:
   * <ul>
   * <li>Número C/C = 45981-36
   * <li>Neste caso: Dígito Verificador da Ag/Conta = 6
   * </ul>
   */
  private String agenciaContaDV;

  /**
   * Versão do Layout do Header do Arquivo
   */
  private String layout;

  /**
   * Coleção com os lotes encontrados dentro do arquivo
   */
  private List<CNAB240Lote> lotes = new ArrayList<CNAB240Lote>();

  /**
   * G015 Código Remessa / Retorno<br>
   * Código adotado pela FEBRABAN para qualificar o envio ou devolução de arquivo entre a Empresa Cliente e o Banco prestador dos Serviços. <br>
   * Domínio:
   * <li>'1' = Remessa (Cliente > Banco)
   * <li>'2' = Retorno (Banco > Cliente)
   */
  private String remessaRetorno;

  // Mantém a referência para o último lote criado
  private CNAB240Lote lastLote = null;

  // Mantém a referência para o último registro de retalhe criado.
  private CNAB240RegisterDetail lastRegister = null;

  /**
   * Instancia um objeto que representa um arquivo de retorno CNAB240.
   *
   * @param retFile InputStream com o conteúdo do arquivo.
   * @throws RFWException
   */
  public CNAB240Parser(InputStream retFile) throws RFWException {
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(retFile))) {
      String line;
      int countLine = 0;
      int lastRegisterType = -1; // Marca o último registro apra saber se o registro atual é esperado

      while ((line = reader.readLine()) != null) {
        countLine++;
        if ("".equals(line)) continue;

        if (line.length() != 240) throw new RFWValidationException("A linha '${0}' não contém 240 caracteres!", new String[] { "" + countLine });

        String tipoRegistro = line.substring(7, 8);

        // Separa a linha de acordo com o tipo de registro:
        switch (tipoRegistro) {
          case "0": // Header do Arquivo
            if (lastRegisterType != -1) {
              throw new RFWValidationException("O registro do tipo '0' é esperado na primeira linha do arquivo. Encontrado na linha '${0}'.", new String[] { "" + countLine });
            }
            processFileHeader(line, countLine);
            break;
          case "1": // Header do Lote
            if (lastRegisterType != 0 && lastRegisterType != 5) {
              throw new RFWValidationException("O registro do tipo '1' é esperado após a abertura do arquivo ou após o fechamento de outro lote. Encontrado na linha '${0}'.", new String[] { "" + countLine });
            }
            lastLote = new CNAB240Lote(line);
            this.lotes.add(lastLote);
            break;
          case "3": // Detalhe
            if (lastRegisterType != 3 && lastRegisterType != 1) {
              throw new RFWValidationException("O registro do tipo '3' é esperado após a abertura do lote ou após outro registro detalhe. Encontrado na linha '${0}'.", new String[] { "" + countLine });
            }
            String segmento = line.substring(13, 14);
            switch (segmento) {
              case "J":
                if (" ".equals(line.substring(14, 15)) && "52".equals(line.substring(17, 19))) { // Utiliza o Branco obrigatório do registro 52, que não é branco no registro J principal, para idenficicar se é um registro J ou o complementar J-52
                  if (lastRegister == null || !(lastRegister instanceof CNAB240RegisterJ)) {
                    throw new RFWCriticalException("Encontrado um registro detalhe J-52 sem um registro J predecessor na linha '${0}'.", new String[] { "" + countLine });
                  }
                  ((CNAB240RegisterJ) lastRegister).addLine(line);
                  lastRegister = null; // Não permite outras adições de linha SubRegistros no mesmo J
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
                RFWLogger.logDebug("Registro Segmento '${0}' ignorado por não haver suporte no parser!", new String[] { segmento });
                break;
            }
            break;
          case "5": // Trailer do Lote
            if (lastRegisterType != 3) {
              throw new RFWValidationException("O registro do tipo '5' é esperado após o lançamento de registros detalhes. Encontrado na linha '${0}'.", new String[] { "" + countLine });
            }
            this.lastLote = null;
            this.lastRegister = null;
            break;
          case "9": // Trailer do Arquivo
            if (lastRegisterType != 5) {
              throw new RFWValidationException("O registro do tipo '9' é esperado após o fechamento dos lotes. Encontrado na linha '${0}'.", new String[] { "" + countLine });
            }
            this.lastLote = null;
            break;
          default:
            throw new RFWValidationException("Tipo de registro '${0}' desconhecido encontrado na linha'${1}'.", new String[] { tipoRegistro, "" + countLine });
        }
        lastRegisterType = Integer.parseInt(tipoRegistro);
      }
    } catch (IOException e) {
      throw new RFWCriticalException("Falha ao lêr conteúdo do arquivo!", e);
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
    // No da Versão do Layout do Arquivo 164-166 3 - Num
    this.layout = line.substring(163, 166);
    if (this.layout.equals("103")) {
      // Código do Banco na Compensação 1-3 3 - Num
      this.codigoBanco = line.substring(0, 3);
      // Tipo de Inscrição da Empresa 18-18 1 - Num
      this.tipoInscricao = line.substring(17, 18);
      // Número de Inscrição da Empresa 19-32 14 - Num
      this.numeroInscricao = line.substring(18, 32);
      // Agência Mantenedora da Conta 53-57 5 - Num
      this.agencia = line.substring(52, 57);
      // Dígito Verificador da Agência 58-58 1 - Alfa
      this.agenciaDV = line.substring(57, 58);
      // Número da Conta Corrente 59-70 12 - Num
      this.contaNumero = line.substring(58, 70);
      // Dígito Verificador da Conta 71-71 1 - Alfa
      this.contaDV = line.substring(70, 71);
      // Dígito Verificador da Ag/Conta 72-72 1 - Alfa
      this.agenciaContaDV = line.substring(71, 72);
      // Código Remessa / Retorno 143 143 1 - Num
      this.remessaRetorno = line.substring(142, 143);
      // Número Seqüencial do Arquivo 158 163 6 - Num
      this.numeroSequencialArquivo = line.substring(157, 163);
      // Para Uso Reservado da Empresa 192 211 20 - Alfa
      this.reservadoEmpresa = line.substring(191, 211);
    } else {
      throw new RFWCriticalException("Layout de Header de Lote desconhecido: '${0}'.", new String[] { this.layout });
    }
  }

  /**
   * # g001 Código do Banco na Compensação<br>
   * Código fornecido pelo Banco Central para identificação do Banco que está recebendo ou enviando o arquivo, com o qual se firmou o contrato de prestação de serviços. <br>
   * Preencher com “988” quando a transferência for efetuada para outra instituição financeira utilizando o código ISPB. Neste caso, deverá ser preenchido o código ISPB no campo 26.3B.
   *
   * @return the g001 Código do Banco na Compensação<br>
   *         Código fornecido pelo Banco Central para identificação do Banco que está recebendo ou enviando o arquivo, com o qual se firmou o contrato de prestação de serviços
   */
  public String getCodigoBanco() {
    return codigoBanco;
  }

  /**
   * # g001 Código do Banco na Compensação<br>
   * Código fornecido pelo Banco Central para identificação do Banco que está recebendo ou enviando o arquivo, com o qual se firmou o contrato de prestação de serviços. <br>
   * Preencher com “988” quando a transferência for efetuada para outra instituição financeira utilizando o código ISPB. Neste caso, deverá ser preenchido o código ISPB no campo 26.3B.
   *
   * @param codigoBanco the new g001 Código do Banco na Compensação<br>
   *          Código fornecido pelo Banco Central para identificação do Banco que está recebendo ou enviando o arquivo, com o qual se firmou o contrato de prestação de serviços
   */
  public void setCodigoBanco(String codigoBanco) {
    this.codigoBanco = codigoBanco;
  }

  /**
   * # g005 Tipo de Inscrição da Empresa <br>
   * Código que identifica o tipo de inscrição da Empresa ou Pessoa Física perante uma Instituição governamental.Domínio:
   * <ul>
   * <li>'0' = Isento / Não Informado
   * <li>'1' = CPF
   * <li>'2' = CGC / CNPJ
   * <li>'3' = PIS / PASEP
   * <li>'9' = Outros
   * </ul>
   * <li>Preenchimento deste campo é obrigatório para DOC e TED (Forma de Lançamento = 03, 41, 43).
   * <li>Para pagamento para o SIAPE com crédito em conta, o CPF deverá ser do 1º titular
   * <li>Para o Produto/Serviço Cobrança considerar como obrigatório, a partir de 01.06.2015, somente o CPF (código 1) ou o CNPJ (código 2). Os demais códigos não deverão ser utilizados.
   *
   * @return the g005 Tipo de Inscrição da Empresa <br>
   *         Código que identifica o tipo de inscrição da Empresa ou Pessoa Física perante uma Instituição governamental
   */
  public String getTipoInscricao() {
    return tipoInscricao;
  }

  /**
   * # g005 Tipo de Inscrição da Empresa <br>
   * Código que identifica o tipo de inscrição da Empresa ou Pessoa Física perante uma Instituição governamental.Domínio:
   * <ul>
   * <li>'0' = Isento / Não Informado
   * <li>'1' = CPF
   * <li>'2' = CGC / CNPJ
   * <li>'3' = PIS / PASEP
   * <li>'9' = Outros
   * </ul>
   * <li>Preenchimento deste campo é obrigatório para DOC e TED (Forma de Lançamento = 03, 41, 43).
   * <li>Para pagamento para o SIAPE com crédito em conta, o CPF deverá ser do 1º titular
   * <li>Para o Produto/Serviço Cobrança considerar como obrigatório, a partir de 01.06.2015, somente o CPF (código 1) ou o CNPJ (código 2). Os demais códigos não deverão ser utilizados.
   *
   * @param tipoInscricao the new g005 Tipo de Inscrição da Empresa <br>
   *          Código que identifica o tipo de inscrição da Empresa ou Pessoa Física perante uma Instituição governamental
   */
  public void setTipoInscricao(String tipoInscricao) {
    this.tipoInscricao = tipoInscricao;
  }

  /**
   * # g006 Número de Inscrição da Empresa<br>
   * Deve ser preenchido de acordo com o tipo de inscrição definido no campo {@link #tipoInscricao}.<br>
   * Número de inscrição da Empresa ou Pessoa Física perante uma Instituição governamental. <br>
   * Quando o Tipo de Inscrição for igual a zero (não informado), preencher com zeros.
   *
   * @return the g006 Número de Inscrição da Empresa<br>
   *         Deve ser preenchido de acordo com o tipo de inscrição definido no campo {@link #tipoInscricao}
   */
  public String getNumeroInscricao() {
    return numeroInscricao;
  }

  /**
   * # g006 Número de Inscrição da Empresa<br>
   * Deve ser preenchido de acordo com o tipo de inscrição definido no campo {@link #tipoInscricao}.<br>
   * Número de inscrição da Empresa ou Pessoa Física perante uma Instituição governamental. <br>
   * Quando o Tipo de Inscrição for igual a zero (não informado), preencher com zeros.
   *
   * @param numeroInscricao the new g006 Número de Inscrição da Empresa<br>
   *          Deve ser preenchido de acordo com o tipo de inscrição definido no campo {@link #tipoInscricao}
   */
  public void setNumeroInscricao(String numeroInscricao) {
    this.numeroInscricao = numeroInscricao;
  }

  /**
   * # g007 Código do Convênio no Banco<br>
   * Código adotado pelo Banco para identificar o Contrato entre este e a Empresa Cliente. G <Br>
   * .
   *
   * @return the g007 Código do Convênio no Banco<br>
   *         Código adotado pelo Banco para identificar o Contrato entre este e a Empresa Cliente
   */
  public String getCodigoConvenio() {
    return codigoConvenio;
  }

  /**
   * # g007 Código do Convênio no Banco<br>
   * Código adotado pelo Banco para identificar o Contrato entre este e a Empresa Cliente. G <Br>
   * .
   *
   * @param codigoConvenio the new g007 Código do Convênio no Banco<br>
   *          Código adotado pelo Banco para identificar o Contrato entre este e a Empresa Cliente
   */
  public void setCodigoConvenio(String codigoConvenio) {
    this.codigoConvenio = codigoConvenio;
  }

  /**
   * # g008 Agência Mantenedora da Conta<br>
   * Código adotado pelo Banco responsável pela conta, para identificar a qual unidade está vinculada a conta corrente.<Br>
   * Refêre-se à agência da conta que receberá o arquivo de lote.
   *
   * @return the g008 Agência Mantenedora da Conta<br>
   *         Código adotado pelo Banco responsável pela conta, para identificar a qual unidade está vinculada a conta corrente
   */
  public String getAgencia() {
    return agencia;
  }

  /**
   * # g008 Agência Mantenedora da Conta<br>
   * Código adotado pelo Banco responsável pela conta, para identificar a qual unidade está vinculada a conta corrente.<Br>
   * Refêre-se à agência da conta que receberá o arquivo de lote.
   *
   * @param agencia the new g008 Agência Mantenedora da Conta<br>
   *          Código adotado pelo Banco responsável pela conta, para identificar a qual unidade está vinculada a conta corrente
   */
  public void setAgencia(String agencia) {
    this.agencia = agencia;
  }

  /**
   * # g009 Dígito Verificador da Agência<Br>
   * Código adotado pelo Banco responsável pela conta corrente, para verificação da autenticidade do Código da Agência.<br>
   * Não definir se o banco não utilizar DV para a agência.
   *
   * @return the g009 Dígito Verificador da Agência<Br>
   *         Código adotado pelo Banco responsável pela conta corrente, para verificação da autenticidade do Código da Agência
   */
  public String getAgenciaDV() {
    return agenciaDV;
  }

  /**
   * # g009 Dígito Verificador da Agência<Br>
   * Código adotado pelo Banco responsável pela conta corrente, para verificação da autenticidade do Código da Agência.<br>
   * Não definir se o banco não utilizar DV para a agência.
   *
   * @param agenciaDV the new g009 Dígito Verificador da Agência<Br>
   *          Código adotado pelo Banco responsável pela conta corrente, para verificação da autenticidade do Código da Agência
   */
  public void setAgenciaDV(String agenciaDV) {
    this.agenciaDV = agenciaDV;
  }

  /**
   * # g010 Número da Conta Corrente<br>
   * Número adotado pelo Banco, para identificar univocamente a conta corrente utilizada pelo Cliente.
   *
   * @return the g010 Número da Conta Corrente<br>
   *         Número adotado pelo Banco, para identificar univocamente a conta corrente utilizada pelo Cliente
   */
  public String getContaNumero() {
    return contaNumero;
  }

  /**
   * # g010 Número da Conta Corrente<br>
   * Número adotado pelo Banco, para identificar univocamente a conta corrente utilizada pelo Cliente.
   *
   * @param contaNumero the new g010 Número da Conta Corrente<br>
   *          Número adotado pelo Banco, para identificar univocamente a conta corrente utilizada pelo Cliente
   */
  public void setContaNumero(String contaNumero) {
    this.contaNumero = contaNumero;
  }

  /**
   * # g011 Dígito Verificador da Conta<br>
   * Código adotado pelo responsável pela conta corrente, para verificação da autenticidade do Número da Conta Corrente.<br>
   * Para os Bancos que se utilizam de duas posições para o Dígito Verificador do Número da Conta Corrente, preencher este campo com a 1ª posição deste dígito.<br>
   * Exemplo :
   * <ul>
   * <li>Número C/C = 45981-36
   * <li>Neste caso: Dígito Verificador da Conta = 3
   * </ul>
   * <b>ATENÇÃO:</B>A maioria doa bancos não utiliza esse DV de conta separado, mas sim o DV do conjunto Agência + Conta, que deve ser definido no {@link #agenciaContaDV}.
   *
   * @return the g011 Dígito Verificador da Conta<br>
   *         Código adotado pelo responsável pela conta corrente, para verificação da autenticidade do Número da Conta Corrente
   */
  public String getContaDV() {
    return contaDV;
  }

  /**
   * # g011 Dígito Verificador da Conta<br>
   * Código adotado pelo responsável pela conta corrente, para verificação da autenticidade do Número da Conta Corrente.<br>
   * Para os Bancos que se utilizam de duas posições para o Dígito Verificador do Número da Conta Corrente, preencher este campo com a 1ª posição deste dígito.<br>
   * Exemplo :
   * <ul>
   * <li>Número C/C = 45981-36
   * <li>Neste caso: Dígito Verificador da Conta = 3
   * </ul>
   * <b>ATENÇÃO:</B>A maioria doa bancos não utiliza esse DV de conta separado, mas sim o DV do conjunto Agência + Conta, que deve ser definido no {@link #agenciaContaDV}.
   *
   * @param contaDV the new g011 Dígito Verificador da Conta<br>
   *          Código adotado pelo responsável pela conta corrente, para verificação da autenticidade do Número da Conta Corrente
   */
  public void setContaDV(String contaDV) {
    this.contaDV = contaDV;
  }

  /**
   * # g012 Dígito Verificador da Agência / Conta Corrente<br>
   * Código adotado pelo Banco responsável pela conta corrente, para verificação da autenticidade do par Código da Agência / Número da Conta Corrente.<br>
   * Para os Bancos que se utilizam de duas posições para o Dígito Verificador do Número da Conta Corrente, preencher este campo com a 2ª posição deste dígito.<br>
   * Exemplo:
   * <ul>
   * <li>Número C/C = 45981-36
   * <li>Neste caso: Dígito Verificador da Ag/Conta = 6
   * </ul>
   * .
   *
   * @return the g012 Dígito Verificador da Agência / Conta Corrente<br>
   *         Código adotado pelo Banco responsável pela conta corrente, para verificação da autenticidade do par Código da Agência / Número da Conta Corrente
   */
  public String getAgenciaContaDV() {
    return agenciaContaDV;
  }

  /**
   * # g012 Dígito Verificador da Agência / Conta Corrente<br>
   * Código adotado pelo Banco responsável pela conta corrente, para verificação da autenticidade do par Código da Agência / Número da Conta Corrente.<br>
   * Para os Bancos que se utilizam de duas posições para o Dígito Verificador do Número da Conta Corrente, preencher este campo com a 2ª posição deste dígito.<br>
   * Exemplo:
   * <ul>
   * <li>Número C/C = 45981-36
   * <li>Neste caso: Dígito Verificador da Ag/Conta = 6
   * </ul>
   * .
   *
   * @param agenciaContaDV the new g012 Dígito Verificador da Agência / Conta Corrente<br>
   *          Código adotado pelo Banco responsável pela conta corrente, para verificação da autenticidade do par Código da Agência / Número da Conta Corrente
   */
  public void setAgenciaContaDV(String agenciaContaDV) {
    this.agenciaContaDV = agenciaContaDV;
  }

  /**
   * # g018 Número Seqüencial do Arquivo<br>
   * Número seqüencial adotado e controlado pelo responsável pela geração do arquivo para ordenar a disposição dos arquivos encaminhados. <br>
   * Evoluir um número seqüencial a cada header de arquivo.
   *
   * @return the g018 Número Seqüencial do Arquivo<br>
   *         Número seqüencial adotado e controlado pelo responsável pela geração do arquivo para ordenar a disposição dos arquivos encaminhados
   */
  public String getNumeroSequencialArquivo() {
    return numeroSequencialArquivo;
  }

  /**
   * # g018 Número Seqüencial do Arquivo<br>
   * Número seqüencial adotado e controlado pelo responsável pela geração do arquivo para ordenar a disposição dos arquivos encaminhados. <br>
   * Evoluir um número seqüencial a cada header de arquivo.
   *
   * @param numeroSequencialArquivo the new g018 Número Seqüencial do Arquivo<br>
   *          Número seqüencial adotado e controlado pelo responsável pela geração do arquivo para ordenar a disposição dos arquivos encaminhados
   */
  public void setNumeroSequencialArquivo(String numeroSequencialArquivo) {
    this.numeroSequencialArquivo = numeroSequencialArquivo;
  }

  /**
   * # g022 Para Uso Reservado da Empresa<br>
   * Texto de observações destinado para uso exclusivo da Empresa.
   *
   * @return the g022 Para Uso Reservado da Empresa<br>
   *         Texto de observações destinado para uso exclusivo da Empresa
   */
  public String getReservadoEmpresa() {
    return reservadoEmpresa;
  }

  /**
   * # g022 Para Uso Reservado da Empresa<br>
   * Texto de observações destinado para uso exclusivo da Empresa.
   *
   * @param reservadoEmpresa the new g022 Para Uso Reservado da Empresa<br>
   *          Texto de observações destinado para uso exclusivo da Empresa
   */
  public void setReservadoEmpresa(String reservadoEmpresa) {
    this.reservadoEmpresa = reservadoEmpresa;
  }

  /**
   * # versão do Layout do Header do Arquivo.
   *
   * @return the versão do Layout do Header do Arquivo
   */
  public String getLayout() {
    return layout;
  }

  /**
   * # versão do Layout do Header do Arquivo.
   *
   * @param layout the new versão do Layout do Header do Arquivo
   */
  public void setLayout(String layout) {
    this.layout = layout;
  }

  /**
   * # coleção com os lotes encontrados dentro do arquivo.
   *
   * @return the coleção com os lotes encontrados dentro do arquivo
   */
  public List<CNAB240Lote> getLotes() {
    return lotes;
  }

  /**
   * # coleção com os lotes encontrados dentro do arquivo.
   *
   * @param lotes the new coleção com os lotes encontrados dentro do arquivo
   */
  public void setLotes(List<CNAB240Lote> lotes) {
    this.lotes = lotes;
  }

  /**
   * # g015 Código Remessa / Retorno<br>
   * Código adotado pela FEBRABAN para qualificar o envio ou devolução de arquivo entre a Empresa Cliente e o Banco prestador dos Serviços. <br>
   * Domínio:
   * <li>'1' = Remessa (Cliente > Banco)
   * <li>'2' = Retorno (Banco > Cliente).
   *
   * @return the g015 Código Remessa / Retorno<br>
   *         Código adotado pela FEBRABAN para qualificar o envio ou devolução de arquivo entre a Empresa Cliente e o Banco prestador dos Serviços
   */
  public String getRemessaRetorno() {
    return remessaRetorno;
  }

  /**
   * # g015 Código Remessa / Retorno<br>
   * Código adotado pela FEBRABAN para qualificar o envio ou devolução de arquivo entre a Empresa Cliente e o Banco prestador dos Serviços. <br>
   * Domínio:
   * <li>'1' = Remessa (Cliente > Banco)
   * <li>'2' = Retorno (Banco > Cliente).
   *
   * @param remessaRetorno the new g015 Código Remessa / Retorno<br>
   *          Código adotado pela FEBRABAN para qualificar o envio ou devolução de arquivo entre a Empresa Cliente e o Banco prestador dos Serviços
   */
  public void setRemessaRetorno(String remessaRetorno) {
    this.remessaRetorno = remessaRetorno;
  }

  /**
   * Transforma o resultado do método {@link #getRemessaRetorno()} em booleano de acordo com a operação de Remessa.
   *
   * @return true se for um arquivo de remessa, false se for um arquivo de retorno, null se o métoro original retornar null ou tiver um valor desconhecido.
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

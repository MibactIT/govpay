package it.govpay.backoffice.v1.controllers;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.time.DateUtils;
import org.openspcoop2.utils.json.ValidationException;
import org.slf4j.Logger;
import org.springframework.security.core.Authentication;

import it.govpay.backoffice.v1.beans.ListaStatisticheQuadrature;
import it.govpay.backoffice.v1.beans.RaggruppamentoStatistica;
import it.govpay.backoffice.v1.beans.StatisticaQuadratura;
import it.govpay.backoffice.v1.beans.TipoRiscossione;
import it.govpay.backoffice.v1.beans.converter.StatisticaQuadraturaConverter;
import it.govpay.bd.reportistica.statistiche.model.StatisticaRiscossione;
import it.govpay.core.dao.anagrafica.dto.BasicFindRequestDTO;
import it.govpay.core.dao.reportistica.StatisticaRiscossioniDAO;
import it.govpay.core.dao.reportistica.dto.ListaRiscossioniDTO;
import it.govpay.core.dao.reportistica.dto.ListaRiscossioniDTO.GROUP_BY;
import it.govpay.core.dao.reportistica.dto.ListaRiscossioniDTOResponse;
import it.govpay.core.utils.SimpleDateFormatUtils;
import it.govpay.model.Acl.Diritti;
import it.govpay.model.Acl.Servizio;
import it.govpay.model.Pagamento.TipoPagamento;
import it.govpay.model.Utenza.TIPO_UTENZA;
import it.govpay.model.reportistica.statistiche.FiltroRiscossioni;


public class QuadratureController extends BaseController {

	public QuadratureController(String nomeServizio,Logger log) {
		super(nomeServizio,log);
	}

	public Response getQuadratureRiscossioni(Authentication user, UriInfo uriInfo, HttpHeaders httpHeaders , Integer pagina, Integer risultatiPerPagina, 
			String dataDa, String dataA, List<String> idDominio, List<String> idUnita, List<String> idTipoPendenza, List<String> direzione, List<String> divisione, List<String> tassonomia, List<String> tipo, List<String> gruppi) {
		String methodName = "getQuadratureRiscossioni";
		String transactionId = this.context.getTransactionId();

		try{
			this.log.debug(MessageFormat.format(BaseController.LOG_MSG_ESECUZIONE_METODO_IN_CORSO, methodName)); 

			// autorizzazione sulla API
			this.isAuthorized(user, Arrays.asList(TIPO_UTENZA.OPERATORE, TIPO_UTENZA.APPLICAZIONE), Arrays.asList(Servizio.RENDICONTAZIONI_E_INCASSI), Arrays.asList(Diritti.LETTURA));

			// Parametri - > DTO Input

			ListaRiscossioniDTO listaRiscossioniDTO = new ListaRiscossioniDTO(user);

			FiltroRiscossioni filtro = new FiltroRiscossioni();

			if(risultatiPerPagina == null) {
				listaRiscossioniDTO.setLimit(BasicFindRequestDTO.DEFAULT_LIMIT);
			} else {
				listaRiscossioniDTO.setLimit(risultatiPerPagina);
			}

			if(pagina == null) {
				listaRiscossioniDTO.setPagina(1);
			} else {
				listaRiscossioniDTO.setPagina(pagina);
			}

			Date dataDaDate = null;
			if(dataDa!=null) {
				dataDaDate = DateUtils.parseDate(dataDa, SimpleDateFormatUtils.datePatternsRest.toArray(new String[0]));
				Calendar c = Calendar.getInstance();
				c.setTime(dataDaDate);
				c.set(Calendar.HOUR_OF_DAY, 0);
				c.set(Calendar.MINUTE, 0);
				c.set(Calendar.SECOND, 0);
				c.set(Calendar.MILLISECOND, 0);

				filtro.setDataDa(c.getTime());
			}

			Date dataADate = null;
			if(dataA!=null) {
				dataADate = DateUtils.parseDate(dataA, SimpleDateFormatUtils.datePatternsRest.toArray(new String[0]));
				Calendar c = Calendar.getInstance();
				c.setTime(dataADate);
				c.set(Calendar.HOUR_OF_DAY, 25); 
				c.set(Calendar.MINUTE, 59);
				c.set(Calendar.SECOND, 59);
				c.set(Calendar.MILLISECOND, 999);

				filtro.setDataA(c.getTime());
			}

			filtro.setCodDominio(idDominio);
			filtro.setCodUo(idUnita);
			filtro.setCodTipoVersamento(idTipoPendenza);
			filtro.setDirezione(direzione);
			filtro.setDivisione(divisione);
			filtro.setTassonomia(tassonomia);
			
			if(tipo!=null) {
				List<TipoPagamento> tipi = new ArrayList<TipoPagamento>();
				for (String tipoS : tipo) {
					TipoRiscossione tipoRiscossione = TipoRiscossione.fromValue(tipoS);
					if(tipoRiscossione != null) {
						tipi.add(TipoPagamento.valueOf(tipoRiscossione.toString()));
					}
				}
				
				if(tipi.size()> 0) {
					filtro.setTipo(tipi);
				}
			}

			if(gruppi != null && gruppi.size() >0) {
				List<GROUP_BY> groupBy = new ArrayList<ListaRiscossioniDTO.GROUP_BY>();
				for (String gruppoString : gruppi) {
					RaggruppamentoStatistica gruppo = RaggruppamentoStatistica.fromValue(gruppoString);
					if(gruppo != null) {
						GROUP_BY gruppoToAdd = null;

						switch (gruppo) {
						case APPLICAZIONE:
							gruppoToAdd = GROUP_BY.APPLICAZIONE;
							break;
						case DIREZIONE:
							gruppoToAdd = GROUP_BY.DIR;
							break;
						case DIVISIONE:
							gruppoToAdd = GROUP_BY.DIV;
							break;
						case DOMINIO:
							gruppoToAdd = GROUP_BY.DOMINIO;
							break;
						case TIPO_PENDENZA:
							gruppoToAdd = GROUP_BY.TIPO_PENDENZA;
							break;
						case UNITA_OPERATIVA:
							gruppoToAdd = GROUP_BY.UO;
							break;
						case TASSONOMIA:
							gruppoToAdd = GROUP_BY.TASSONOMIA;
							break;
						}

						if(groupBy.contains(gruppoToAdd))
							throw new ValidationException("Il gruppo [" + gruppoString + "] e' stato indicato piu' di una volta.");

						groupBy.add(gruppoToAdd);

					} else {
						throw new ValidationException("Codifica inesistente per gruppo. Valore fornito [" + gruppoString + "] valori possibili " + ArrayUtils.toString(RaggruppamentoStatistica.values()));
					}
				}
				listaRiscossioniDTO.setGroupBy(groupBy);
			} else {
				throw new ValidationException("Indicare almeno un gruppo");
			}

			listaRiscossioniDTO.setFiltro(filtro);

			// INIT DAO

			StatisticaRiscossioniDAO statisticaRiscossioniDAO = new StatisticaRiscossioniDAO(); 

			// CHIAMATA AL DAO

			ListaRiscossioniDTOResponse listaRiscossioniDTOResponse = statisticaRiscossioniDAO.listaRiscossioni(listaRiscossioniDTO);

			// CONVERT TO JSON DELLA RISPOSTA

			List<StatisticaQuadratura> results = new ArrayList<>();
			for(StatisticaRiscossione entrataPrevista: listaRiscossioniDTOResponse.getResults()) {
				StatisticaQuadratura rsModel = StatisticaQuadraturaConverter.toRsModelIndex(entrataPrevista); 
				results.add(rsModel);
			} 

			ListaStatisticheQuadrature response = new ListaStatisticheQuadrature(results, this.getServicePath(uriInfo), listaRiscossioniDTOResponse.getTotalResults(), listaRiscossioniDTO.getPagina(), listaRiscossioniDTO.getLimit());

			this.log.debug(MessageFormat.format(BaseController.LOG_MSG_ESECUZIONE_METODO_COMPLETATA, methodName)); 
			return this.handleResponseOk(Response.status(Status.OK).entity(response.toJSON(null)),transactionId).build();
		}catch (Exception e) {
			return this.handleException(uriInfo, httpHeaders, methodName, e, transactionId);
		} finally {
			this.log(this.context);
		}
	}
}



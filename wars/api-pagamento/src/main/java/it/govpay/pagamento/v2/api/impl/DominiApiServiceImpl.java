package it.govpay.pagamento.v2.api.impl;

import it.govpay.core.dao.anagrafica.DominiDAO;
import it.govpay.core.dao.anagrafica.dto.FindDominiDTO;
import it.govpay.core.dao.anagrafica.dto.FindDominiDTOResponse;
import it.govpay.pagamento.v2.api.*;
import it.govpay.pagamento.v2.beans.Domini;
import it.govpay.pagamento.v2.beans.converter.DominiConverter;

import org.openspcoop2.utils.jaxrs.impl.BaseImpl;
import org.openspcoop2.utils.jaxrs.impl.ServiceContext;

import org.openspcoop2.utils.jaxrs.fault.FaultCode;
/**
 * GovPay - API Pagamento
 *
 * <p>No description provided (generated by Swagger Codegen https://github.com/swagger-api/swagger-codegen)
 *
 */
public class DominiApiServiceImpl extends BaseImpl implements DominiApi {

	public DominiApiServiceImpl(){
		super(org.slf4j.LoggerFactory.getLogger(DominiApiServiceImpl.class));
	}

	/**
	 * Lettura del logo di un dominio
	 *
	 */
	@Override
	public String getLogo(String idDominio) {
		ServiceContext context = this.getContext();
		try {
			context.getLogger().info("Invocazione in corso ...");     

			// TODO: Implement...

			context.getLogger().info("Invocazione completata con successo");
			return null;

		}
		catch(javax.ws.rs.WebApplicationException e) {
			context.getLogger().error("Invocazione terminata con errore '4xx': %s",e, e.getMessage());
			throw e;
		}
		catch(Throwable e) {
			context.getLogger().error("Invocazione terminata con errore: %s",e, e.getMessage());
			throw FaultCode.ERRORE_INTERNO.toException(e);
		}
	}

	/**
	 * Elenco dei domini
	 *
	 */
	@Override
	public Domini findDomini(Integer offset, Integer limit, String fields, String sort) {
		
		/* default values */
		if(offset == null || offset < 0) offset = 0;
		if(limit == null || limit < 0 || limit > 100) limit = 25;
		
		ServiceContext context = this.getContext();
		try {
			context.getLogger().info("Invocazione in corso ...");     

			context.getLogger().debug("Autorizzazione completata con successo");     

			FindDominiDTO listaDominiDTO = new FindDominiDTO(context.getAuthentication());
			listaDominiDTO.setOffset(offset);
			listaDominiDTO.setLimit(limit);
			listaDominiDTO.setOrderBy(sort);
			listaDominiDTO.setAbilitato(true);

			// INIT DAO
			DominiDAO dominiDAO = new DominiDAO();

			// CHIAMATA AL DAO
			FindDominiDTOResponse listaDominiDTOResponse = dominiDAO.findDomini(listaDominiDTO);

			// CONVERT TO JSON DELLA RISPOSTA

			Domini domini = DominiConverter.toRsModel(listaDominiDTOResponse.getResults(), offset, limit, listaDominiDTOResponse.getTotalResults(), context.getUriInfo());

			context.getLogger().info("Invocazione completata con successo");

			return domini;

		}
		catch(javax.ws.rs.WebApplicationException e) {
			context.getLogger().error("Invocazione terminata con errore '4xx': %s",e, e.getMessage());
			throw e;
		}
		catch(Throwable e) {
			context.getLogger().error("Invocazione terminata con errore: %s",e, e.getMessage());
			throw FaultCode.ERRORE_INTERNO.toException(e);
		}
	}
}


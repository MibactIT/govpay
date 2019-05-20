package it.govpay.backoffice.v1.controllers;

import java.text.MessageFormat;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.springframework.security.core.Authentication;

import it.govpay.backoffice.v1.beans.Profilo;
import it.govpay.backoffice.v1.beans.converter.ProfiloConverter;
import it.govpay.core.dao.anagrafica.UtentiDAO;
import it.govpay.core.dao.anagrafica.dto.LeggiProfiloDTOResponse;

public class ProfiloController extends BaseController {

     public ProfiloController(String nomeServizio,Logger log) {
 		super(nomeServizio,log);
     }

    public Response profiloGET(Authentication user, UriInfo uriInfo, HttpHeaders httpHeaders) {
    	String methodName = "profiloGET";  
		String transactionId = this.context.getTransactionId();
		this.log.debug(MessageFormat.format(BaseController.LOG_MSG_ESECUZIONE_METODO_IN_CORSO, methodName)); 
		try{
			UtentiDAO utentiDAO = new UtentiDAO();
			
			LeggiProfiloDTOResponse leggiProfilo = utentiDAO.getProfilo(user);
			
			Profilo profilo = ProfiloConverter.getProfilo(leggiProfilo);

			this.log.debug(MessageFormat.format(BaseController.LOG_MSG_ESECUZIONE_METODO_COMPLETATA, methodName)); 
			return this.handleResponseOk(Response.status(Status.OK).entity(profilo.toJSON(null)),transactionId).build();
		}catch (Exception e) {
			return this.handleException(uriInfo, httpHeaders, methodName, e, transactionId);
		} finally {
			this.log(this.context);
		}
    }


}



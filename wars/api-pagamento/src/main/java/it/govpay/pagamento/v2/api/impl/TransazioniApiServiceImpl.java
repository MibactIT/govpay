package it.govpay.pagamento.v2.api.impl;

import it.govpay.pagamento.v2.api.*;
import it.govpay.pagamento.v2.beans.EsitoRpp;
import it.govpay.pagamento.v2.beans.Rpp;
import it.govpay.pagamento.v2.beans.Rpps;

import org.openspcoop2.utils.jaxrs.impl.AuthorizationManager;
import org.openspcoop2.utils.jaxrs.impl.BaseImpl;
import org.openspcoop2.utils.jaxrs.impl.ServiceContext;
import org.openspcoop2.utils.jaxrs.impl.AuthorizationConfig;

import javax.ws.rs.core.UriBuilder;

import org.openspcoop2.utils.jaxrs.fault.FaultCode;
/**
 * GovPay - API Pagamento
 *
 * <p>No description provided (generated by Swagger Codegen https://github.com/swagger-api/swagger-codegen)
 *
 */
public class TransazioniApiServiceImpl extends BaseImpl implements TransazioniApi {

	public static UriBuilder basePath = UriBuilder.fromPath("/rpps");
	
	public TransazioniApiServiceImpl(){
		super(org.slf4j.LoggerFactory.getLogger(TransazioniApiServiceImpl.class));
	}

	private AuthorizationConfig getAuthorizationConfig() throws Exception{
		// TODO: Implement ...
		throw new Exception("NotImplemented");
	}

    /**
     * Lista delle richieste di pagamento pendenza
     *
     */
	@Override
    public Rpps findRpps(String idDominio, String iuv, String ccp, String idA2A, String idPendenza, Integer offset, Integer limit, String fields, String sort, String idDebitore, EsitoRpp statoPendenza, String idSessionePortale) {
		ServiceContext context = this.getContext();
		try {
			context.getLogger().info("Invocazione in corso ...");     

			AuthorizationManager.authorize(context, getAuthorizationConfig());
			context.getLogger().debug("Autorizzazione completata con successo");     
                        
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
     * Dettaglio di una richiesta di pagamento pendenza
     *
     */
	@Override
    public Rpp getRpp(String idDominio, String iuv, String ccp) {
		ServiceContext context = this.getContext();
		try {
			context.getLogger().info("Invocazione in corso ...");     

			AuthorizationManager.authorize(context, getAuthorizationConfig());
			context.getLogger().debug("Autorizzazione completata con successo");     
                        
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
     * Acquisizione della richiesta di pagamento pagopa
     *
     */
	@Override
    public Object getRpt(String idDominio, String iuv, String ccp) {
		ServiceContext context = this.getContext();
		try {
			context.getLogger().info("Invocazione in corso ...");     

			AuthorizationManager.authorize(context, getAuthorizationConfig());
			context.getLogger().debug("Autorizzazione completata con successo");     
                        
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
     * Acquisizione della ricevuta di pagamento
     *
     */
	@Override
    public byte[] getRt(String idDominio, String iuv, String ccp) {
		ServiceContext context = this.getContext();
		try {
			context.getLogger().info("Invocazione in corso ...");     

			AuthorizationManager.authorize(context, getAuthorizationConfig());
			context.getLogger().debug("Autorizzazione completata con successo");     
                        
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
    
}


/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package grycap.keycloak.event.listener.datasetservice;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ServerInfoAwareProviderFactory;

/**
 *
 * @author Sergio López Huguet (serlohu@upv.es)
 */
public class DatasetServiceEventListenerProviderFactory implements EventListenerProviderFactory, ServerInfoAwareProviderFactory {
    
    private static final Logger log = Logger.getLogger(DatasetServiceEventListenerProviderFactory.class);
    private Set<String> groups;
    private ResourceType target_event;
    private String datasetService_endpoint, keycloak_client, keycloak_client_secret, keycloak_endpoint, keycloak_realm;
    
    public DatasetServiceEventListenerProvider create(KeycloakSession session) {
        return new DatasetServiceEventListenerProvider(session, this.target_event, this.groups, this.datasetService_endpoint, this.keycloak_client, this.keycloak_client_secret, this.keycloak_endpoint, this.keycloak_realm);
    }
        
    public String getId() {
        return "event-listener-datasetservice";
    }

    public void init(Config.Scope config) {
        groups = new HashSet<>();
        log.info( String.format( "### ------------  %s.init() ------------ ###", this.getId() ) );
        
        String[] group_names = config.get("OIDCGroups","").split(",");
        if (group_names != null) {
            for (String name : group_names) {
                this.groups.add(name);
            }
        }
        this.target_event = ResourceType.valueOf(config.get("adminEvent", "GROUP_MEMBERSHIP" ) );
        this.datasetService_endpoint = config.get("DatasetServiceEndpoint", "");
        this.keycloak_client = config.get("KeycloakClient", "");
        this.keycloak_client_secret = config.get("KeycloakClientSecret", "");
        this.keycloak_endpoint = config.get("KeycloakEndpoint", "https://localhost:8080");
        this.keycloak_realm = config.get("KeycloakRealm", "CHAIMELEON");
        
        log.info ("Configuration variables: ");
        
        log.info ("\tOIDC groups: " + this.groups.toString());
        log.info ("\tAdmin event target: " + this.target_event.toString());
        log.info ("\tDatasetService endpoint: " + this.datasetService_endpoint );
        log.info ("\tKeycloak client: " + this.keycloak_client);
        log.info ("\tKeycloak client secret: " + this.keycloak_client_secret );
        log.info ("\tKeycloak endpoint: " + this.keycloak_endpoint );
        log.info ("\tKeycloak realm: " + this.keycloak_realm );
        
        log.info( String.format("-----------------------------------------------------------") );
    }

    public void postInit(KeycloakSessionFactory factory) {
    }

    public void close() {
    } 
    
    @Override
    public Map<String, String> getOperationalInfo() {
        Map<String, String> ret = new LinkedHashMap<>();
        ret.put("OIDCgroups", this.groups.toString());
        ret.put("AdminEventTarget", this.target_event.toString());
        ret.put("DatasetServiceEndpoint", this.datasetService_endpoint);
        ret.put("KeycloakClient", this.keycloak_client);
        ret.put("KeycloakClientSecret", this.keycloak_client_secret);
        ret.put("KeycloakEndpoint", this.keycloak_endpoint);
        ret.put("KeycloakRealm", this.keycloak_realm);

        //ret.put("KubeauthorizerUserClaim", this.kubeauthorizer_userclaim);
        return ret;
    }
}

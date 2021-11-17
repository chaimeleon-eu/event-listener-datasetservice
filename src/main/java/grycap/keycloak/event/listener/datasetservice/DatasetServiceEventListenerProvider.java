/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package grycap.keycloak.event.listener.datasetservice;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.StringReader;
import java.util.HashSet;
import java.util.ArrayList;
import org.jboss.logging.Logger;
import java.util.Set;
import java.util.logging.Level;

import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.OAuth2Constants;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessTokenResponse;
//import org.keycloak.admin.client.Keycloak;
//import org.keycloak.admin.client.KeycloakBuilder;


import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import javax.json.Json;
import javax.json.JsonReader;
import javax.json.JsonObject;

/**
 *
 * @author Sergio López Huguet (serlohu@upv.es)
 */
public class DatasetServiceEventListenerProvider implements EventListenerProvider{
    
    private static final Logger log = Logger.getLogger(DatasetServiceEventListenerProvider.class);

    private final KeycloakSession session;
    private final RealmProvider model;
    private ResourceType target_event; 
    private Set<String> target_groups; 
    private String datasetService_endpoint, keycloak_client, keycloak_client_secret, keycloak_endpoint, keycloak_realm;
    
    public DatasetServiceEventListenerProvider(KeycloakSession session, ResourceType event, Set<String> groups, String datasetService_endpoint, String keycloak_client, String keycloak_client_secret, String keycloak_endpoint, String keycloak_realm) {
        this.session = session;
        this.model = session.realms();
        this.target_event = event;
        this.target_groups = groups;
        this.datasetService_endpoint = datasetService_endpoint;
        this.keycloak_client = keycloak_client;
        this.keycloak_client_secret = keycloak_client_secret;
        this.keycloak_endpoint = keycloak_endpoint;
        this.keycloak_realm = keycloak_realm;
    }

    @Override
    public void onEvent(Event event) {
    }

    @Override
    public void close() {

    }
    
    private String getAccessToken() {
        String access_token = null;
        String post_endpoint = String.format("%s/auth/realms/%s/protocol/openid-connect/token", this.keycloak_endpoint, this.keycloak_realm) ;

        HttpPost post = new HttpPost(post_endpoint);
        String post_json = String.format( "grant_type=client_credentials&client_id=%s&client_secret=%s", this.keycloak_client, this.keycloak_client_secret );

        StringEntity entity = new StringEntity(post_json, ContentType.APPLICATION_FORM_URLENCODED  );
        post.setEntity(entity);
        
        
        CloseableHttpClient httpclient = HttpClients.createDefault();

        try( CloseableHttpResponse response = httpclient.execute(post) ) {
            HttpEntity response_entity = response.getEntity();
            String responseString = EntityUtils.toString(response_entity, "UTF-8");
            

            if (response.getStatusLine().getStatusCode() == 200) {
                JsonReader jsonReader = Json.createReader(new StringReader(responseString));
                JsonObject data = jsonReader.readObject();
                access_token = data.getString("access_token");
                java.util.logging.Logger.getLogger(DatasetServiceEventListenerProvider.class.getName()).log(Level.FINEST, String.format( "Access token: %s", access_token) );
                jsonReader.close();
            }else {
                java.util.logging.Logger.getLogger(DatasetServiceEventListenerProvider.class.getName()).log(Level.SEVERE, String.format( "Cannot obtain access token -> Keycloak response (status code=%d). Message: %s", response.getStatusLine().getStatusCode(), responseString) );
            }
            
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(DatasetServiceEventListenerProvider.class.getName()).log(Level.SEVERE, null, ex);
        }   
        
        return access_token;
    }

    
    private boolean UserDatasetService(String access_token, String user_name, String user_id,  String user_groups, String new_group){
        boolean result = false;

        String post_endpoint = String.format("%s/user/%s", this.datasetService_endpoint, user_name) ;

        HttpPost post = new HttpPost(post_endpoint);
        String post_json = String.format( "{ \"uid\": \"%s\", \"groups\": %s }", user_id, user_groups );

        StringEntity entity = new StringEntity(post_json, ContentType.APPLICATION_JSON);
        post.setEntity(entity);
        post.addHeader("Accept", "application/json");
        post.addHeader("Content-type", "application/json");
        post.addHeader("Authorization", String.format("bearer %s", access_token));


        CloseableHttpClient httpclient = HttpClients.createDefault();

        try( CloseableHttpResponse response = httpclient.execute(post) ) {
            HttpEntity response_entity = response.getEntity();
            String responseString = EntityUtils.toString(response_entity, "UTF-8");
            if (response.getStatusLine().getStatusCode() != 201){
                java.util.logging.Logger.getLogger(DatasetServiceEventListenerProvider.class.getName()).log(Level.SEVERE, String.format( "Dataset service response (status code=%d). Message: %s", response.getStatusLine().getStatusCode(), responseString) );
            }else{
                //log.info(String.format( "User %s added to group %s at Dataset Service successfully", user_name));
                result = true;
            }
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(DatasetServiceEventListenerProvider.class.getName()).log(Level.SEVERE, null, ex);
        }   

        return result;
    }
    /*
    private boolean deleteUserDatasetService(String access_token, String user_name, String user_id,  String user_groups, String new_group){
        boolean result = false;

        String post_endpoint = String.format("%s/user/%s", this.datasetService_endpoint, user_name) ;

        HttpDelete delete = new HttpDelete(post_endpoint);
        String post_json = String.format( "{ \"uid\": \"%s\", \"groups\": %s }", user_id, user_groups );

        StringEntity entity = new StringEntity(post_json, ContentType.APPLICATION_JSON);
        delete.setEntity(entity);
        delete.addHeader("Accept", "application/json");
        delete.addHeader("Content-type", "application/json");
        delete.addHeader("Authorization", String.format("bearer %s", access_token));


        CloseableHttpClient httpclient = HttpClients.createDefault();

        try( CloseableHttpResponse response = httpclient.execute(delete) ) {
            HttpEntity response_entity = response.getEntity();
            String responseString = EntityUtils.toString(response_entity, "UTF-8");
            if (response.getStatusLine().getStatusCode() != 201){
                java.util.logging.Logger.getLogger(DatasetServiceEventListenerProvider.class.getName()).log(Level.SEVERE, String.format( "Dataset service response (status code=%d). Message: %s", response.getStatusLine().getStatusCode(), responseString) );
            }else{
                log.info(String.format( "User %s is deleted from group %s at Dataset Service successfully", user_name));
            }
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(DatasetServiceEventListenerProvider.class.getName()).log(Level.SEVERE, null, ex);
        }   

        return result;
    }*/

    @Override
    public void onEvent(AdminEvent event, boolean includeRepresentation) {
        String user_id, user_name, user_email, group_id, group_name;
        String post_endpoint, post_json;
        ArrayList <String> user_groups = new ArrayList<String>();
        RealmModel realm = this.model.getRealm(event.getRealmId());       

        
        if ( target_event.equals(event.getResourceType()) ){
            log.info( String.format( "### ------------ NEW ADMIN ENVENT - %s - %s ------------ ###", event.getResourceType().toString(), event.getOperationType().toString() ) );
            
            user_id = event.getResourcePath().split("/")[1];
            user_name = this.session.users().getUserById(realm, user_id).getUsername();
            user_email = this.session.users().getUserById(realm, user_id).getEmail();
            group_id = event.getResourcePath().split("/")[3];
            group_name = realm.getGroupById(group_id).getName() ;
            
            this.session.users().getUserById(realm, user_id).getGroupsStream().forEach((g)-> {
                user_groups.add(String.format( "\"%s\"" , g.getName()) );
            });
            
            if (target_groups.contains(group_name)) {
                String access_token = this.getAccessToken();
                
                if (access_token != null) {
                    boolean result = this.UserDatasetService(access_token, user_name, user_id, user_groups.toString(), group_name);
                    if (event.getOperationType().equals(OperationType.CREATE) && (result==true) ) {
                        log.info( String.format( "User %s (%s) added to %s (%s) group", user_name, user_id, group_name, group_id) );
                        
                    } else if ( event.getOperationType().equals(OperationType.DELETE) && (result==true) ) {
                        log.info( String.format( "User %s (%s) deleted from %s (%s) group", user_name, user_id, group_name, group_id) );
                    }
                
                
                }
                
           
            }
            
            log.info("-----------------------------------------------------------");   
        }
        
       
                
    }
    
}

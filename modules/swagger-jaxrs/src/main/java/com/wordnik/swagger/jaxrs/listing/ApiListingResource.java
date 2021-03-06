package com.wordnik.swagger.jaxrs.listing;

import com.wordnik.swagger.config.*;
import com.wordnik.swagger.core.filter.*;
import com.wordnik.swagger.jaxrs.Reader;
import com.wordnik.swagger.jaxrs.config.*;
import com.wordnik.swagger.util.*;
import com.wordnik.swagger.models.Swagger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.ws.rs.*;
import javax.ws.rs.core.*;

import java.util.Set;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Path("/")
public class ApiListingResource {
  static boolean initialized = false;
  @Context
  ServletContext context;

  protected synchronized void scan (Application app, ServletConfig sc) {
    Scanner scanner = ScannerFactory.getScanner();
    System.out.println("using scanner " + scanner);
    if(scanner != null) {
      Set<Class<?>> classes = null;
      if (scanner instanceof JaxrsScanner) {
        JaxrsScanner jaxrsScanner = (JaxrsScanner)scanner;
        classes = jaxrsScanner.classesFromContext(app, sc);
      }
      else {
        classes = scanner.classes();
      }
      if(classes != null) {
        Reader reader = new Reader((Swagger)context.getAttribute("swagger"));
        Swagger swagger = reader.read(classes);
        context.setAttribute("swagger", swagger);
        WebXMLReader xmlReader = (WebXMLReader)context.getAttribute("reader");
        if(xmlReader != null) {
          xmlReader.read(swagger);
        }
      }
    }
    initialized = true;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/swagger.json")
  public Response getListingJson(
    @Context Application app,
    @Context ServletConfig sc,
    @Context HttpHeaders headers,
    @Context UriInfo uriInfo) {
    Swagger swagger = (Swagger) context.getAttribute("swagger");
    if(!initialized) 
      scan(app, sc);
    if(swagger != null) {
      SwaggerSpecFilter filterImpl = FilterFactory.getFilter();
      if(filterImpl != null) {
        SpecFilter f = new SpecFilter();
        swagger = f.filter(swagger,
          filterImpl,
          getQueryParams(uriInfo.getQueryParameters()),
          getCookies(headers),
          getHeaders(headers));
      }
      return Response.ok().entity(swagger).build();
    }
    else
      return Response.status(404).build();
  }

  @GET
  @Produces("application/yaml")
  @Path("/swagger.yaml")
  public Response getListingYaml(
    @Context Application app,
    @Context ServletConfig sc,
    @Context HttpHeaders headers,
    @Context UriInfo uriInfo) {
    Swagger swagger = (Swagger) context.getAttribute("swagger");
    if(!initialized) 
      scan(app, sc);
    try{
      if(swagger != null) {
        SwaggerSpecFilter filterImpl = FilterFactory.getFilter();
        System.out.println("using filter " + filterImpl);
        if(filterImpl != null) {
          SpecFilter f = new SpecFilter();
          swagger = f.filter(swagger,
            filterImpl,
            getQueryParams(uriInfo.getQueryParameters()),
            getCookies(headers),
            getHeaders(headers));
        }

        String yaml = Yaml.mapper().writeValueAsString(swagger);
        String[] parts = yaml.split("\n");
        StringBuilder b = new StringBuilder();
        for(String part : parts) {
          int pos = part.indexOf("!<");
          int endPos = part.indexOf(">");
          // dirty hack for https://github.com/FasterXML/jackson-dataformat-yaml/issues/22
          // if(pos >= 0)
          //   b.append(part.replace("!<", "in: ").replace(">", ""));
          // else
            b.append(part);
          b.append("\n");
        }
        return Response.ok().entity(b.toString()).type("text/plain").build();
      }
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    return Response.status(404).build();
  }

  protected Map<String, List<String>> getQueryParams(MultivaluedMap<String, String> params) {
    Map<String, List<String>> output = new HashMap<String, List<String>>();
    if(params != null) {
      for(String key: params.keySet()) {
        List<String> values = params.get(key);
        output.put(key, values);
      }
    }
    return output;
  }

  protected Map<String, String> getCookies(HttpHeaders headers) {
    Map<String, String> output = new HashMap<String, String>();
    if(headers != null) {
      for(String key: headers.getCookies().keySet()) {
        Cookie cookie = headers.getCookies().get(key);
        output.put(key, cookie.getValue());
      }
    }
    return output;
  }


  protected Map<String, List<String>> getHeaders(HttpHeaders headers) {
    Map<String, List<String>> output = new HashMap<String, List<String>>();
    if(headers != null) {
      for(String key: headers.getRequestHeaders().keySet()) {
        List<String> values = headers.getRequestHeaders().get(key);
        output.put(key, values);
      }
    }
    return output;
  }
}
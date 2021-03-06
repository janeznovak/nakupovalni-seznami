package si.fri.prpo.nakupovanje.api.v1.viri;

import com.kumuluz.ee.configuration.utils.ConfigurationUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import si.fri.prpo.nakupovanje.storitve.bean.ArtikelBean;
import si.fri.prpo.nakupovanje.entitete.Artikel;
import com.kumuluz.ee.rest.beans.QueryParameters;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Context;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

@ApplicationScoped
@Path("artikli")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)

public class ArtikelVir {

    private Client httpClient;
    private String baseUrl;
    private String baseUrlcat;
    private ConfigurationUtil conf=ConfigurationUtil.getInstance();
    private Logger log=Logger.getLogger(ArtikelVir.class.getName());
    private JSONParser jp=new JSONParser();

    @Context
    protected UriInfo uriInfo;

    @Inject
    private ArtikelBean aBean;

    @PostConstruct
    public void init(){
        httpClient = ClientBuilder.newClient();
        baseUrl = conf.get("predlogeapiurl").get();
        baseUrlcat=conf.get("kategorijeapiurl").get();
        log.info("baseurl: "+baseUrl);
        log.info("baseurlcat: "+baseUrlcat);
    }

    @Operation(description = "Vrne seznam priporočenih artiklov", summary = "Seznam priporočenih artiklov",
        tags = "artikli", responses = {
        @ApiResponse(responseCode = "200",
            description = "Seznam priporočenih artiklov",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = Artikel.class))),
            headers = {@Header(name = "X-Total-Count", description = "Število vrnjenih artiklov")})
    })
    @GET
    @Path("predloge")
    public Response pridobiPredloge(){
        Integer[] response=httpClient.target(baseUrl+"/predloge").request().get(Integer[].class);
        List<Artikel> artikli=new ArrayList<>();
        for(int i=0;i<response.length;i++){
            artikli.add(aBean.getArtikel(response[i]));
        }
        int count=response.length;
        return Response.ok(artikli).header("X-Total-Count", count).build();
    }

    @Operation(description = "Vrne seznam artiklov", summary = "Seznam artiklov",
            tags = "artikli", responses = {
            @ApiResponse(responseCode = "200",
                    description = "Seznam artiklov",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = Artikel.class))),
                    headers = {@Header(name = "X-Total-Count", description = "Število vrnjenih artiklov")})
    })
    @GET
    public Response pridobiArtikle(){

        QueryParameters query = QueryParameters.query(uriInfo.getRequestUri().getQuery()).build();
        Long artCount = aBean.getArtikliCount(query);
        return Response
                .ok(aBean.getArtikli(query))
                .header("X-Total-Count", artCount)
                .build();

    }

    @Operation(description = "Vrne podrobnosti artikla", summary = "Podrobnosti artikla",
            tags = "artikli", responses = {
            @ApiResponse(responseCode = "200",
                    description = "Podrobnosti artikla",
                    content = @Content(schema = @Schema(implementation = Artikel.class))
            )
    })
    @GET
    @Path("{id}")
    public Response pridobiArtikel(@PathParam("id") Integer id) {

        Artikel ns = aBean.getArtikel(id);

        if(ns != null){
            return Response.ok(ns).build();
        }else{
            return  Response.status(Response.Status.NOT_FOUND).build();
        }

    }

    @Operation(description = "Dodaj artikel", summary = "Dodajanje artikla",
            tags = "artikli", responses = {
            @ApiResponse(responseCode = "201",
                    description = "Artikel uspešno dodan"
            ),
            @ApiResponse(responseCode = "405", description = "Validacijska napaka")
    })
    @POST
    public Response dodajArtikel(Artikel a) throws ParseException{
        log.info(baseUrlcat);
        Response res=httpClient.target(baseUrlcat)
            .queryParam("text",a.getNaziv())
            .request(MediaType.APPLICATION_JSON_TYPE)
            .accept(MediaType.APPLICATION_JSON_TYPE)
            .header("x-rapidapi-host", "twinword-twinword-bundle-v1.p.rapidapi.com")
            .header("x-rapidapi-key", "0770ff268fmshaceafaef6d368d5p15e5a4jsne89c956312ec")
            .get();
        String body=res.readEntity(String.class);
        JSONObject jo=(JSONObject)jp.parse(body);
        JSONArray msg=(JSONArray)jo.get("categories");
        log.info("REZULTAT POIZVEDBE:");
        Iterator<String> iterator=msg.iterator();
        while(iterator.hasNext()){
            log.info(iterator.next());
        }
        return Response
                .status(Response.Status.CREATED)
                .entity(aBean.addArtikel(a))
                .build();
    }

    @Operation(description = "Posodobi artikel", summary = "Posodabljanje artikla",
            tags = "artikli", responses = {
            @ApiResponse(responseCode = "201", description = "Artikel uspešno posodobljen"
            )
    })
    @PUT
    @Path("{id}")
    public Response posodobiArtikel(@PathParam("id") Integer id, Artikel a) {
        return Response
                .status(Response.Status.CREATED)
                .entity(aBean.updateArtikel(id,a))
                .build();
    }

    @Path("{id}")
    public Response zbrisiArtikel(@Parameter(
            description = "Identifikator artikla za brisanje", required = true)
                                  @PathParam("id") Integer id){
        return Response .status(Response.Status.OK)
                .entity(aBean.deleteArtikel(id))
                .build();
    }
    @DELETE
    @Path("{id}")
    public Response odstraniArtikel(@PathParam("id") Integer id) {
        return Response
                .status(Response.Status.OK)
                .entity(aBean.deleteArtikel(id))
                .build();
    }
}

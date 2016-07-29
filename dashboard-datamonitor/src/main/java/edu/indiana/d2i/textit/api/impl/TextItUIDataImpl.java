package edu.indiana.d2i.textit.api.impl;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;
import edu.indiana.d2i.textit.api.TextItUIData;
import edu.indiana.d2i.textit.api.util.Constants;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.ws.rs.*;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Path("/")
public class TextItUIDataImpl extends TextItUIData {
    private WebResource resource;
    private String serviceUrl;
    private CacheControl control = new CacheControl();
	private SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    private WebResource resource(){
        return resource;
    }

	public TextItUIDataImpl() {
		this.serviceUrl = Constants.apiUrl;
		resource = Client.create().resource(serviceUrl);
		control.setNoCache(true);
	}

	public String getLastWeek(String country) {

		Calendar c = Calendar.getInstance();

		if (country.equals("kenya")) {
			c.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
			c.add(Calendar.DATE, -16);
		}else if (country.equals("zambia")){
			c.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
			c.add(Calendar.DATE, -9);
		}

		Date last_country_day = Calendar.getInstance().getTime();
		Date before = c.getTime();
		String start = sdfDate.format(before);
		String end = sdfDate.format(last_country_day);
		return start;
	}

	@GET
	@Path("/{country}/flows")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAllFlows(@PathParam("country") String country) {
        WebResource webResource = resource();

        ClientResponse response = webResource.path(country + "/flows")
                .accept("application/json")
                .type("application/json")
                .get(ClientResponse.class);

        return Response.status(response.getStatus()).entity(response
                .getEntity(new GenericType<String>() {})).cacheControl(control).build();
	}

	@GET
	@Path("/{country}/lastweekflows")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAllLastWeekFlows(@PathParam("country") String country) {
		WebResource webResource = resource();
		String fromDate = getLastWeek(country);

		ClientResponse response = webResource.path(country + "/flows")
				.queryParam("from", fromDate)
				.accept("application/json")
				.type("application/json")
				.get(ClientResponse.class);

		return Response.status(response.getStatus()).entity(response
				.getEntity(new GenericType<String>() {})).cacheControl(control).build();
	}

	@GET
	@Path("/{country}/runs")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAllRuns(@PathParam("country") String country) {
		WebResource webResource = resource();

		ClientResponse response = webResource.path(country + "/runs")
				.accept("application/json")
				.type("application/json")
				.get(ClientResponse.class);

		return Response.status(response.getStatus()).entity(response
				.getEntity(new GenericType<String>() {})).cacheControl(control).build();
	}

	@GET
	@Path("/{country}/lastweekruns")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAllLastWeekRuns(@PathParam("country") String country) {
		WebResource webResource = resource();
		String fromDate = getLastWeek(country);

		ClientResponse response = webResource.path(country + "/runs")
				.queryParam("from", fromDate)
				.accept("application/json")
				.type("application/json")
				.get(ClientResponse.class);

		return Response.status(response.getStatus()).entity(response
				.getEntity(new GenericType<String>() {})).cacheControl(control).build();
	}

	@GET
	@Path("/{country}/contacts")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAllContacts(@PathParam("country") String country) {
		WebResource webResource = resource();

		ClientResponse response = webResource.path(country + "/contacts")
				.accept("application/json")
				.type("application/json")
				.get(ClientResponse.class);

		return Response.status(response.getStatus()).entity(response
				.getEntity(new GenericType<String>() {})).cacheControl(control).build();
	}

	@GET
	@Path("/{country}/lastweekcontacts")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAllLastWeekModifiedContacts(@PathParam("country") String country) {
		WebResource webResource = resource();
		String modified_date = getLastWeek(country);

		ClientResponse response = webResource.path(country + "/contacts")
				.queryParam("from", modified_date)
				.accept("application/json")
				.type("application/json")
				.get(ClientResponse.class);

		return Response.status(response.getStatus()).entity(response
				.getEntity(new GenericType<String>() {})).cacheControl(control).build();
	}

	@GET
	@Path("/{country}/all")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAllData(@PathParam("country") String country) {
		WebResource webResource = resource();

		ClientResponse response = webResource.path(country + "/all")
				.accept("application/json")
				.type("application/json")
				.get(ClientResponse.class);

		return Response.status(response.getStatus()).entity(response
				.getEntity(new GenericType<String>() {})).cacheControl(control).build();
	}

	@GET
	@Path("/{country}/runsofflow")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getRunsOfFlowData(@PathParam("country") String country) {
		WebResource webResource = resource();

		String fromDate = getLastWeek(country);
		Response output = getAllLastWeekFlows(country);
		String str_output = output.getEntity().toString();

		JSONArray allFlows = new JSONArray(str_output);
		JSONObject allRunsofFlows = new JSONObject();

		ClientResponse response = null;
		for (int i = 0; i < allFlows.length(); i++) {
			JSONObject jsonobj = allFlows.getJSONObject(i);

			// get flow_id
			String flow_id = jsonobj.getString("uuid");
			response = webResource.path(country + "/runsofflow")
					.queryParam("flowId", flow_id)
					.accept("application/json")
					.type("application/json")
					.get(ClientResponse.class);

			JSONArray response_array = new JSONArray(response.getEntity(new GenericType<String>() {}));
			allRunsofFlows.put(flow_id, response_array);
		}

		return Response.status(response.getStatus()).entity(allRunsofFlows.toString()).cacheControl(control).build();
	}

	@GET
	@Path("/{country}/runsandcontacts")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getRunsAndContactsData(@PathParam("country") String country) {
		WebResource webResource = resource();

		String fromDate = getLastWeek(country);
		ClientResponse response = webResource.path(country + "/runsandcontacts")
				.queryParam("from", fromDate)
				.accept("application/json")
				.type("application/json")
				.get(ClientResponse.class);

		return Response.status(response.getStatus()).entity(response
				.getEntity(new GenericType<String>() {})).cacheControl(control).build();
	}

	@GET
	@Path("/{country}/quesforanswers")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getQuesForAnswers(@PathParam("country") String country) {
		WebResource webResource = resource();

		Response output = getAllLastWeekFlows(country);
		String str_output = output.getEntity().toString();

		JSONArray allFlows = new JSONArray(str_output);
		JSONArray result_array = new JSONArray();

		ClientResponse response = null;
		if (allFlows.length() == 0) {
			return Response.status(Response.Status.OK).cacheControl(control).build();
		}else {
			for (int i = 0; i < allFlows.length(); i++) {
				JSONObject result = new JSONObject();

				JSONObject flowObject = allFlows.getJSONObject(i);
				String flow_id = (String) flowObject.get("uuid");
				String created_on = (String) flowObject.get("created_on");
				int total = (Integer) flowObject.get("runs");
				Map<String, JSONObject> qMap = new TreeMap<String, JSONObject>();
				JSONArray rulesets = (JSONArray) flowObject.get("rulesets");

				for (int j = 0; j < rulesets.length(); j++) {
					JSONObject rule = rulesets.getJSONObject(j);
					qMap.put((String) rule.get("node"), new JSONObject().put("label", rule.get("label")).put("count", 0));
				}

				response = webResource.path(country + "/runsofflow")
						.queryParam("flowId", flow_id)
						.accept("application/json")
						.type("application/json")
						.get(ClientResponse.class);

				JSONArray runs_array = new JSONArray(response.getEntity(new GenericType<String>() {
				}));

				for (int k = 0; k < runs_array.length(); k++) {
					JSONArray values_array = runs_array.getJSONObject(k).getJSONArray("values");
					for (int l = 0; l < values_array.length(); l++) {
						JSONObject value = values_array.getJSONObject(l);
						String nodeId = value.getString("node");
						int current_count = qMap.get(nodeId).getInt("count");
						qMap.get(nodeId).put("count", ++current_count);
					}
				}

				JSONArray question_array = new JSONArray();
				for (String node : qMap.keySet()) {
					JSONObject qObject = qMap.get(node);
					question_array.put(new JSONObject()
							.put("q_name", qObject.getString("label"))
							.put("ans_count", qObject.getInt("count"))
							.put("total", total));
				}

				result.put("flow_id", flow_id);
				result.put("created_on", created_on);
				result.put("ques_detail", question_array);

				result_array.put(result);

			}

			JSONArray sortedJsonArray = new JSONArray();
			List<JSONObject> jsonList = new ArrayList<JSONObject>();
			for (int i = 0; i < result_array.length(); i++) {
				jsonList.add(result_array.getJSONObject(i));
			}

			Collections.sort(jsonList, new Comparator<JSONObject>() {

				public int compare(JSONObject a, JSONObject b) {
					Date dt = new Date();
					Date dt2 = new Date();

					try {
						dt = sdfDate.parse((String) a.get("created_on"));
						dt2 = sdfDate.parse((String) b.get("created_on"));
					} catch (ParseException e) {
						e.printStackTrace();
					}
					return dt2.compareTo(dt);
				}
			});


			for (int i = 0; i < result_array.length(); i++) {
				sortedJsonArray.put(jsonList.get(i));
			}

			return Response.status(response.getStatus()).entity(sortedJsonArray.toString()).cacheControl(control).build();
		}
	}

	@GET
	@Path("/{country}/activecontacts")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getActiveContactsData(@PathParam("country") String country,
										  @QueryParam("count") Integer count) {
		Response output = getRunsAndContactsData(country);
		String str_output = output.getEntity().toString();

		JSONArray allContacts = new JSONArray(str_output);
		JSONArray result = new JSONArray();

		Map<String, JSONObject> qMap = new HashMap<String, JSONObject>();

		for (int i = 0; i < allContacts.length(); i++) {

			JSONObject contactObject = allContacts.getJSONObject(i);
			String contact_id = (String)contactObject.get("contact_id");

			JSONObject contact_object = null;

			if (qMap.get(contact_id) != null){
				contact_object = qMap.get(contact_id);
			} else {
				String contact_name = (String)contactObject.get("contact_name");
				String contact_number = (String)contactObject.get("contact_phone");
				String created_date = (String)contactObject.get("created_on");

				contact_object = new JSONObject().put("contact_id", contact_id).put("completed_count", 0).put("incompleted_count", 0)
						.put("name", contact_name)
						.put("contact_no", contact_number)
						.put("created_on", created_date);

			}

			Boolean status = (Boolean)contactObject.get("status");

			if(status){
				int completed_count = contact_object.getInt("completed_count");
				contact_object.put("completed_count", ++completed_count);
			}else{
				int incompleted_count = contact_object.getInt("incompleted_count");
				contact_object.put("incompleted_count", ++incompleted_count);
			}

			qMap.put(contact_id, contact_object);
		}

		for( String node : qMap.keySet()) {
			JSONObject qObject = qMap.get(node);
			int total = qObject.getInt("completed_count")+qObject.getInt("incompleted_count");
			double perc = (qObject.getInt("completed_count")*1.0/total) * 100;
			qObject.put("perc", Math.round(perc));
			qObject.put("total", total);

			result.put(qObject);

		}

		JSONArray sortedJsonArray = new JSONArray();
		List<JSONObject> jsonList = new ArrayList<JSONObject>();
		for (int i = 0; i < result.length(); i++) {
			jsonList.add(result.getJSONObject(i));
		}

		Collections.sort( jsonList, new Comparator<JSONObject>() {

			public int compare(JSONObject a, JSONObject b) {
				Long valA=null;
				Long valB=null;
				try {
					valA = (Long) a.get("perc");
					valB = (Long) b.get("perc");
				}
				catch (JSONException e) {
					//do something
				}

				int sort_perc = valB.compareTo(valA);

				if (sort_perc != 0) {
					return sort_perc;
				} else {
					Date dt = new Date();
					Date dt2 = new Date();

					try {
						dt = sdfDate.parse((String) a.get("created_on"));
						dt2 = sdfDate.parse((String) b.get("created_on"));
					} catch (ParseException e) {
						e.printStackTrace();
					}
					return dt2.compareTo(dt);
				}
			}
		});

		if (count != null){
			for (int i = 0; i < count; i++) {
				sortedJsonArray.put(jsonList.get(i));
			}
		}else{
			for (int i = 0; i < result.length(); i++) {
				sortedJsonArray.put(jsonList.get(i));
			}
		}

		return Response.status(Response.Status.OK).entity(sortedJsonArray.toString()).cacheControl(control).build();
	}

	@GET
	@Path("/{country}/flowanalytics")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getQuesDetailsForFlow(@PathParam("country") String country,
										  @QueryParam("flowId") String flow_id) {

		if(flow_id == null) {
			//return Response
		}
		WebResource webResource = resource();

		Map<String, JSONObject> qMap = new TreeMap<String, JSONObject>();

		JSONArray result_array = new JSONArray();
		JSONObject result = new JSONObject();
		ClientResponse response = null;
		response = webResource.path(country + "/runsofflow")
				.queryParam("flowId", flow_id)
				.accept("application/json")
				.type("application/json")
				.get(ClientResponse.class);

		JSONArray runs_array = new JSONArray(response.getEntity(new GenericType<String>() {}));
		int total = runs_array.length();
		for(int k = 0 ; k < runs_array.length() ; k++ ){
			JSONArray values_array = runs_array.getJSONObject(k).getJSONArray("values");
			for(int l = 0 ; l < values_array.length() ; l++ ){

				JSONObject value = values_array.getJSONObject(l);
				JSONObject qObject;
				if (qMap.get((String) value.get("node")) != null) {
					qObject = qMap.get((String) value.get("node"));
				}else {
					qObject = new JSONObject().put("label", value.get("label")).put("count", 0).put("category", new JSONObject());
				}

				JSONObject cat_obj = qObject.getJSONObject("category");

				int current_count = qObject.getInt("count");
				qObject.put("count", ++current_count);

				String cat_val = null;

				if (value.getJSONObject("category").has("base")) {
					cat_val = value.getJSONObject("category").getString("base");
				} else if(value.getJSONObject("category").has("eng")){
					cat_val = value.getJSONObject("category").getString("eng");
				}

				if (cat_val != null){
					if (cat_obj.has(cat_val)){
						int cat_new_val = cat_obj.getInt(cat_val);
						cat_obj.put(cat_val, ++cat_new_val);
					}else{
						cat_obj.put(cat_val, 1);
					}
				}

				qMap.put(value.getString("node"),qObject);
			}
		}

		JSONArray question_array = new JSONArray();
		for(String node : qMap.keySet()) {
			JSONObject qObject = qMap.get(node);
			question_array.put(new JSONObject()
					.put("q_name",qObject.getString("label"))
					.put("response_count",qObject.getInt("count"))
					.put("no_response_count",total-qObject.getInt("count"))
					.put("ans",qObject.get("category"))
					.put("total", total));
		}

		result.put("flow_id", flow_id);
		result.put("ques_detail", question_array);

		result_array.put(result);

		return Response.status(response.getStatus()).entity(result_array.toString()).cacheControl(control).build();

	}

	@GET
	@Path("/{country}/flowcompletion")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getFlowCompletionAnalysis(@PathParam("country") String country,
											  @QueryParam("from") String fromDate,
											  @QueryParam("to") String toDate) {
		WebResource webResource = resource();

		ClientResponse response = webResource.path(country + "/flowcompletion")
				.queryParam("from", fromDate)
				.queryParam("to", toDate)
				.accept("application/json")
				.type("application/json")
				.get(ClientResponse.class);

		return Response.status(response.getStatus()).entity(response
				.getEntity(new GenericType<String>() {})).cacheControl(control).build();
	}

	@GET
	@Path("/{country}/filesize")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getFileSizes(@PathParam("country") String country,
								 @QueryParam("count") String count) {
		WebResource webResource = resource();

		ClientResponse response = webResource.path(country + "/filesize")
				.queryParam("count", count)
				.accept("application/json")
				.type("application/json")
				.get(ClientResponse.class);

		return Response.status(response.getStatus()).entity(response
				.getEntity(new GenericType<String>() {})).cacheControl(control).build();
	}

	@GET
	@Path("/{country}/questionanalysis")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getQuestionAnalysis(@PathParam("country") String country,
										@QueryParam("type") String qType,
										@QueryParam("from") String fromDate,
										@QueryParam("to") String toDate) {
		WebResource webResource = resource();

		ClientResponse response = webResource.path(country + "/questionanalysis")
				.queryParam("type", qType)
				.queryParam("from", fromDate)
				.queryParam("to", toDate)
				.accept("application/json")
				.type("application/json")
				.get(ClientResponse.class);

		return Response.status(response.getStatus()).entity(response
				.getEntity(new GenericType<String>() {})).cacheControl(control).build();
	}
}

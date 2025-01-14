package gov.nist.toolkit.session.server.services;

import gov.nist.toolkit.registrymetadata.client.AnyIds;
import gov.nist.toolkit.registrymetadata.client.ObjectRefs;
import gov.nist.toolkit.results.CommonService;
import gov.nist.toolkit.results.client.Result;
import gov.nist.toolkit.sitemanagement.client.SiteSpec;
import gov.nist.toolkit.results.client.TestInstance;
import gov.nist.toolkit.session.server.Session;
import gov.nist.toolkit.xdsexception.client.XdsException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GetAssociations extends CommonService {
	String returnType = "LeafClass";
	Session session;

	public GetAssociations(Session session) throws XdsException {
		this.session = session;
	}

	public List<Result> run(SiteSpec site, ObjectRefs ids) {
		try {
			session.setSiteSpec(site);

			TestInstance testInstance = new TestInstance("GetAssociations", session.getTestSession());
			List<String> sections = new ArrayList<String>();
			Map<String, String> params = new HashMap<String, String>();
			params.put("$returnType$", returnType);

			String prefix;
				prefix = "id";
			
			for (int i=0; i<ids.objectRefs.size(); i++) {
				params.put("$" + prefix + i + "$", ids.objectRefs.get(i).id);
			}
			try {
				if (session.siteSpec.isRG()) {
					sections.add("XCA");
					String home = site.homeId;
					if (home != null && !home.equals("")) {
						params.put("$home$", home);
					}
				}
				else if (session.siteSpec.isIG()) {
					sections.add("IG");
					List<Result> results = session.queryServiceManager().perCommunityQuery(new AnyIds(ids), testInstance, sections, params);
					session.clear();
					return results;
				}
				else {
					sections.add("XDS");
					return asList(session.xdsTestServiceManager().xdstest(testInstance, sections, params, null, null, false));
				}
			} catch (Exception e) {
				return buildResultList(e);
			}
			List<Result> results = session.queryServiceManager().perCommunityQuery(new AnyIds(ids), testInstance, sections, params);
			return results;
		} catch (Exception e) {
			return buildExtendedResultList(e);
		} finally {
			session.clear();
		}
	}
	
}

package gov.nist.toolkit.fhir.simulators.sim.reg.sq;

import gov.nist.toolkit.registrymetadata.Metadata;
import gov.nist.toolkit.registrysupport.logging.LoggerException;
import gov.nist.toolkit.fhir.simulators.sim.reg.store.Fol;
import gov.nist.toolkit.fhir.simulators.sim.reg.store.MetadataCollection;
import gov.nist.toolkit.fhir.simulators.sim.reg.store.RegIndex;
import gov.nist.toolkit.valregmsg.registry.storedquery.generic.GetFolders;
import gov.nist.toolkit.valregmsg.registry.storedquery.generic.QueryReturnType;
import gov.nist.toolkit.valregmsg.registry.storedquery.support.StoredQuerySupport;
import gov.nist.toolkit.xdsexception.client.MetadataException;
import gov.nist.toolkit.xdsexception.client.XdsException;

import java.util.ArrayList;
import java.util.List;

public class GetFoldersSim extends GetFolders {
	RegIndex ri;

	public void setRegIndex(RegIndex ri) {
		this.ri = ri;
	}


	public GetFoldersSim(StoredQuerySupport sqs) {
		super(sqs);
	}

	protected Metadata runImplementation() throws MetadataException,
	XdsException, LoggerException {

		MetadataCollection mc = ri.getMetadataCollection();

		Metadata m = new Metadata();
		m.setVersion3();

		if (fol_uuid != null) {
			if (sqs.returnType == QueryReturnType.LEAFCLASS || sqs.returnType == QueryReturnType.LEAFCLASSWITHDOCUMENT) {
				m = mc.loadRo(fol_uuid);
			} else {
				m.mkObjectRefs(fol_uuid);
			}
		} else if (fol_uid != null) {
			List<Fol> des = new ArrayList<Fol>();
			for (String uid : fol_uid) {
				List<Fol> fs = mc.folCollection.getByUid(uid);
				des.addAll(fs);
			}
			
			List<String> uuidList = new ArrayList<String>();
			for (Fol f : des) {
				uuidList.add(f.getId());
			}
			if (sqs.returnType == QueryReturnType.LEAFCLASS || sqs.returnType == QueryReturnType.LEAFCLASSWITHDOCUMENT) {
				m = mc.loadRo(uuidList);
			} else {
				m.mkObjectRefs(uuidList);
			}
		} else if (fol_lid != null) {
			List<Fol> des = new ArrayList<Fol>();
			for (String lid : fol_lid) {
				List<Fol> fs = mc.folCollection.getByLid(lid);
				des.addAll(fs);
			}

			List<String> uuidList = new ArrayList<String>();
			for (Fol f : des) {
				uuidList.add(f.getId());
			}
			if (sqs.returnType == QueryReturnType.LEAFCLASS || sqs.returnType == QueryReturnType.LEAFCLASSWITHDOCUMENT) {
				m = mc.loadRo(uuidList);
			} else {
				m.mkObjectRefs(uuidList);
			}
		}

		return m;
	}

}

package gov.nist.toolkit.xdstools2.client.inspector;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import gov.nist.toolkit.registrymetadata.client.AnyIds;
import gov.nist.toolkit.registrymetadata.client.ObjectRef;
import gov.nist.toolkit.registrymetadata.client.ObjectRefs;
import gov.nist.toolkit.results.client.Result;
import gov.nist.toolkit.results.client.TestInstance;
import gov.nist.toolkit.xdstools2.client.command.command.GetSubmissionSetsCommand;
import gov.nist.toolkit.xdstools2.client.util.ClientUtils;
import gov.nist.toolkit.xdstools2.shared.command.request.GetSubmissionSetsRequest;

import java.util.List;


public class GetSubmissionSets implements ClickHandler {

	MetadataInspectorTab it;
	ObjectRefs ids;

	void run() {
		/*it.data.*/
		new GetSubmissionSetsCommand(){
			@Override
			public void onFailure(Throwable caught) {
				Result result = Result.RESULT(new TestInstance("GetSubmissionSets"));
				result.assertions.add(caught.getMessage());
				it.addToHistory(result);
			}
			@Override
			public void onComplete(List<Result> result) {
				it.addToHistory(result);
			}
		}.run(new GetSubmissionSetsRequest(ClientUtils.INSTANCE.getCommandContext(),it.getSiteSpec(), new AnyIds(ids)));
	}

	public GetSubmissionSets(MetadataInspectorTab it, ObjectRefs ids) {
		this.it = it;
		this.ids = ids;
	}

	public GetSubmissionSets(MetadataInspectorTab it, ObjectRef id) {
		this.it = it;
		ids = new ObjectRefs();
		ids.objectRefs.add(id);
	}

	public void onClick(ClickEvent event) {
		run();
	}

}

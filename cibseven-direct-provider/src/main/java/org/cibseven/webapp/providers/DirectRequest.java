package org.cibseven.webapp.providers;

import java.util.Date;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import jakarta.ws.rs.core.Variant;

public class DirectRequest implements jakarta.ws.rs.core.Request{

	private Variant variant;
	public DirectRequest() {
		this.variant = Variant.mediaTypes(MediaType.APPLICATION_JSON_TYPE).add().build().get(0);
	}
	@Override
	public String getMethod() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Variant selectVariant(List<Variant> variants) {
		if (variants == null || variants.isEmpty() || this.variant == null) {
			return null;
		}
		for (Variant v : variants) {
			if (v.getMediaType().equals(this.variant.getMediaType())
					&& v.getLanguage().equals(this.variant.getLanguage())
					&& v.getEncoding().equals(this.variant.getEncoding())) {
				return v;
			}
		}
		return null;
	}

	@Override
	public ResponseBuilder evaluatePreconditions(EntityTag eTag) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResponseBuilder evaluatePreconditions(Date lastModified) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResponseBuilder evaluatePreconditions(Date lastModified, EntityTag eTag) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResponseBuilder evaluatePreconditions() {
		// TODO Auto-generated method stub
		return null;
	}

}

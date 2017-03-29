package org.rutebanken.helper.organisation;


import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

/**
 * Representation of RoleAssignments. A list of these should be included in JWT as attribute "roles" under other claims.
 *
 * Short attr names to keep JWT small.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RoleAssignment {

	/**
	 * Private code for role, required
	 */
	public String r;

	/**
	 * Private code for organisation, required
	 */
	public String o;

	/**
	 * Private code for administrative zone, optional
	 */
	public String z;


	/**
	 * Map of entity types (Stop place, PlaceOfInterest ... ) mapped against classifiers for the type (tramStop etc), each represented by private code. Optional.
	 */
	public Map<String, List<String>> e;
}

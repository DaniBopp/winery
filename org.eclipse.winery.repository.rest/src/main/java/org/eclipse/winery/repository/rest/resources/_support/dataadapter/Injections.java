/*******************************************************************************
 * Copyright (c) 2017 University of Stuttgart.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * and the Apache License 2.0 which both accompany this distribution,
 * and are available at http://www.eclipse.org/legal/epl-v20.html
 * and http://www.apache.org/licenses/LICENSE-2.0
 *
 * Contributors:
 *     Karoline Saatkamp - initial API and implementation
 *******************************************************************************/

package org.eclipse.winery.repository.rest.resources._support.dataadapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;

public class Injections {

	@XmlElement
	private List<Injection> injection = new ArrayList<>();

	public List<Injection> getInjections() {
		return Collections.unmodifiableList(injection);
	}

	public void addInjection (Injection in) {
		injection.add(in);
	}
}

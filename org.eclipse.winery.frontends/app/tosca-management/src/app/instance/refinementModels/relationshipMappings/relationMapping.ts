/********************************************************************************
 * Copyright (c) 2018-2020 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache Software License 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *******************************************************************************/

import { RefinementMappings } from '../RefinementMappings';

export enum RelationDirection {
    INGOING = 'INGOING',
    OUTGOING = 'OUTGOING'
}

export class RelationMapping extends RefinementMappings {

    public static readonly idPrefix = 'relMap';

    public relationType: string;
    public direction: RelationDirection;
    public validSourceOrTarget: string;

    constructor(id: number) {
        super(id);
    }

    idPrefix(): string {
        return RelationMapping.idPrefix;
    }
}

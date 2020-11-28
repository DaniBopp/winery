/*******************************************************************************
 * Copyright (c) 2020 Contributors to the Eclipse Foundation
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

import { Component, OnInit } from '@angular/core';
import { WineryDynamicTableMetadata } from '../../../wineryDynamicTable/wineryDynamicTableMetadata';

@Component({
    selector: 'winery-behavior-pattern-mappings',
    templateUrl: './behavior-pattern-mappings.component.html',
    styleUrls: []
})
export class BehaviorPatternMappingsComponent implements OnInit {

    loading = false;
    dynamicTableData: Array<WineryDynamicTableMetadata> = [];
    // TODO
    behaviorPatternMappings: any[] = [];

    constructor() {
    }

    ngOnInit() {
    }

    // TODO
    save(mapping: any) {

    }

    // TODO
    remove(mapping: any) {

    }
}

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
import { BehaviorPatternMapping } from './behaviorPatternMapping';
import { RefinementMappingsService } from '../refinementMappings.service';
import { forkJoin } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { WineryNotificationService } from '../../../wineryNotificationModule/wineryNotification.service';
import { WineryTemplate } from '../../../model/wineryComponent';
import { WineryTableColumn } from '../../../wineryTableModule/wineryTable.component';
import { InstanceService } from '../../instance.service';

@Component({
    templateUrl: './behavior-pattern-mappings.component.html',
    providers: [
        RefinementMappingsService
    ]
})
export class BehaviorPatternMappingsComponent implements OnInit {

    loading = true;
    columns: Array<WineryTableColumn> = [
        { title: 'Detector Element', name: 'detectorElement', sort: true },
        { title: 'Behavior Pattern', name: 'behaviorPattern', sort: true },
        { title: 'Refinement Element', name: 'refinementElement', sort: true },
        { title: 'Refinement Element Property', name: 'refinementProperty', sort: true },
    ];
    behaviorPatternMappings: BehaviorPatternMapping[];

    detectorTemplates: WineryTemplate[];
    behaviorPatterns: any[];
    refinementTemplates: WineryTemplate[];
    refinementProperties: any[];

    constructor(private service: RefinementMappingsService,
                private notify: WineryNotificationService,
                public sharedData: InstanceService) {
    }

    ngOnInit() {
        forkJoin(
            this.service.getBehaviorPatternMappings(),
            this.service.getDetectorNodeTemplates(),
            this.service.getDetectorRelationshipTemplates(),
            this.service.getRefinementTopologyNodeTemplates(),
            this.service.getRefinementTopologyRelationshipTemplates()
        ).subscribe(
            data => this.handleData(data),
            error => this.handleError(error)
        );
    }

    onAddButtonClicked() {
    }

    onRemoveButtonClicked(mapping: BehaviorPatternMapping) {
    }

    private handleData(data: [BehaviorPatternMapping[], WineryTemplate[], WineryTemplate[], WineryTemplate[], WineryTemplate[]]) {
        this.loading = false;
        this.behaviorPatternMappings = data[0];
        this.detectorTemplates = data[1].concat(data[2]);
        this.refinementTemplates = data[3].concat(data[4]);
    }

    private handleError(error: HttpErrorResponse) {
        this.loading = false;
        this.notify.error(error.message);
    }
}

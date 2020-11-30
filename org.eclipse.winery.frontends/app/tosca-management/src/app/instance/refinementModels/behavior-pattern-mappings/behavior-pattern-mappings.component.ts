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

import { Component, OnInit, ViewChild } from '@angular/core';
import { BehaviorPatternMapping } from './behaviorPatternMapping';
import { RefinementMappingsService } from '../refinementMappings.service';
import { forkJoin } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { WineryNotificationService } from '../../../wineryNotificationModule/wineryNotification.service';
import { Policy, WineryTemplateWithPolicies } from '../../../model/wineryComponent';
import { WineryTableColumn } from '../../../wineryTableModule/wineryTable.component';
import { InstanceService } from '../../instance.service';
import { BsModalRef, BsModalService, ModalDirective } from 'ngx-bootstrap';
import { SelectData } from '../../../model/selectData';

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
    detectorTemplates: WineryTemplateWithPolicies[];
    refinementTemplates: WineryTemplateWithPolicies[];

    @ViewChild('addModal') addModal: ModalDirective;
    @ViewChild('removeModal') removeModal: ModalDirective;
    addModalRef: BsModalRef;
    removeModalRef: BsModalRef;

    mapping: BehaviorPatternMapping;
    selectedDetectorElement: WineryTemplateWithPolicies;
    behaviorPatterns: Policy[];
    selectedRefinementElement: WineryTemplateWithPolicies;
    // TODO: remove any
    refinementProperties: any[];

    constructor(private service: RefinementMappingsService,
                private notify: WineryNotificationService,
                public sharedData: InstanceService,
                private modalService: BsModalService) {
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
        const id = this.service.getNewMappingsId(this.behaviorPatternMappings, BehaviorPatternMapping.idPrefix);
        this.mapping = new BehaviorPatternMapping(id);
        this.addModalRef = this.modalService.show(this.addModal);
    }

    detectorElementSelected(node: SelectData) {
        this.mapping.detectorElement = node.id;
        this.selectedDetectorElement = this.detectorTemplates
            .find(value => value.id === node.id);
        if (this.selectedDetectorElement.policies) {
            // TODO: filter for only behavior pattern policies
            this.behaviorPatterns = this.selectedDetectorElement.policies.policy;
        } else {
            delete this.behaviorPatterns;
        }
    }

    refinementElementSelected(node: SelectData) {
        this.mapping.refinementElement = node.id;
        this.selectedRefinementElement = this.refinementTemplates
            .find(value => value.id === node.id);

        // TODO: remove any?
        const props: any = this.selectedRefinementElement.properties;
        if (props && props.propertyType && props.propertyType === 'KV') {
            this.refinementProperties = Object.keys(props.kvproperties)
                .map(key => {
                    // TODO: proper type
                    return {
                        key,
                        value: props.kvproperties[key],
                        str: key + ': ' + props.kvproperties[key]
                    };
                });
        } else {
            delete this.refinementProperties;
        }
    }

    onAddBehaviorPatternMapping() {
        this.loading = true;
        this.service.addBehaviorPatternMapping(this.mapping)
            .subscribe(
                data => this.handleSave('Added', data),
                error => this.handleError(error)
            );
    }

    onRemoveButtonClicked(mapping: BehaviorPatternMapping) {
        this.mapping = mapping;
        this.removeModalRef = this.modalService.show(this.removeModal);
    }

    onRemoveBehaviorPatternMapping() {
        this.service.deleteBehaviorPatternMapping(this.mapping)
            .subscribe(
                data => this.handleSave('Removed', data),
                error => this.handleError(error)
            );
    }

    private handleData(data: [BehaviorPatternMapping[], WineryTemplateWithPolicies[], WineryTemplateWithPolicies[],
        WineryTemplateWithPolicies[], WineryTemplateWithPolicies[]]) {
        this.loading = false;
        this.behaviorPatternMappings = data[0];
        this.detectorTemplates = data[1].concat(data[2]);
        this.refinementTemplates = data[3].concat(data[4]);
    }

    private handleError(error: HttpErrorResponse) {
        this.loading = false;
        this.notify.error(error.message);
    }

    private handleSave(type: string, data: BehaviorPatternMapping[]) {
        this.notify.success(type + ' Behavior Pattern Mapping ' + this.mapping.id);
        this.behaviorPatternMappings = data;
        this.loading = false;
    }
}

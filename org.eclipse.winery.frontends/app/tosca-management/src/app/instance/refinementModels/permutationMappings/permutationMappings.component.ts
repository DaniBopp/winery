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
import { RefinementMappingsService } from '../refinementMappings.service';
import { PermutationMapping } from './permutationMapping';
import { WineryNotificationService } from '../../../wineryNotificationModule/wineryNotification.service';
import { WineryDynamicTableMetadata } from '../../../wineryDynamicTable/wineryDynamicTableMetadata';
import { Validators } from '@angular/forms';
import { DynamicDropdownData } from '../../../wineryDynamicTable/formComponents/dynamicDropdown.component';
import { NodeTemplate, RelationshipTemplate, WineryTemplate } from '../../../model/wineryComponent';
import { forkJoin } from 'rxjs';

@Component({
    templateUrl: 'permutationMappings.component.html',
    providers: [
        RefinementMappingsService
    ]
})
export class PermutationMappingsComponent implements OnInit {
    loading = true;

    dynamicTableData: Array<WineryDynamicTableMetadata> = [];
    permutationMappings: PermutationMapping[] = [];

    tableTitle = 'Permutation Mappings';
    public modalTitle: 'Add Permutation Mapping';

    detectorElements: WineryTemplate[];
    refinementElements: NodeTemplate[];

    detectorElementsTableData: { label: string, value: string }[] = [];
    refinementElementsTableData: { label: string, value: string }[] = [];

    constructor(private service: RefinementMappingsService,
                private notify: WineryNotificationService) {
    }

    // TODO: show empty Table + Modal Title + Delete

    ngOnInit(): void {
        forkJoin(
            this.service.getPermutationMappings(),
            this.service.getDetectorNodeTemplates(),
            this.service.getDetectorRelationshipTemplates(),
            this.service.getRefinementTopologyNodeTemplates()
        ).subscribe(
            data => this.handleData(data),
            error => this.handleError(error)
        );

        this.dynamicTableData = [
            new DynamicDropdownData<string>(
                'detectorElement',
                'Detector Element',
                this.detectorElementsTableData,
                1,
                '',
                [Validators.required],
            ),
            new DynamicDropdownData<string>(
                'refinementElement',
                'Refinement Element',
                this.refinementElementsTableData,
                1,
                '',
                [Validators.required],
            )
        ]
        ;
    }

    private handleError(error: any) {
        this.loading = false;
        this.notify.error(error.message);
    }

    save(): void {
        this.loading = true;
        this.service.addPermutationMappings(this.permutationMappings)
            .subscribe(
                data => this.handleSave('Added', data),
                error => this.handleError(error)
            );
    }

    onChangeProperty() {
        this.save();
    }

    private handleData(data: [PermutationMapping[], NodeTemplate[], RelationshipTemplate[], NodeTemplate[]]) {
        this.permutationMappings = data[0];
        console.log(data[0]);
        this.detectorElements = data[1];
        this.detectorElements.concat(data[2]);

        this.refinementElements = data[3];

        this.detectorElements.forEach((element) => {
                this.detectorElementsTableData.push({ label: element.name, value: element.id }
                );
            }, this
        );
        this.refinementElements.forEach((element) => {
                this.refinementElementsTableData.push({ label: element.name, value: element.id }
                );
            }, this
        );
        this.loading = false;
    }

    private handleSave(added: string, data: PermutationMapping[]) {
        this.notify.success(added + ' Permutation Mapping ');
        this.permutationMappings = data;
        this.loading = false;
    }
}

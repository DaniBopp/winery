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
import { DynamicTextData } from '../../../wineryDynamicTable/formComponents/dynamicText.component';
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
    permutationMappings: PermutationMapping[];
    tableTitle = 'Permutation Mappings';
    modalTitle: 'Add a Permutation Mapping';

    detectorElements: WineryTemplate[];

    constructor(private service: RefinementMappingsService, private notify: WineryNotificationService, ) {
    }

    ngOnInit(): void {
        this.service.getPermutationMappings().subscribe(
            data => {
                this.loading = false;
                this.permutationMappings = data;
            },
            error => this.handleError(error)
        );
        console.log(this.permutationMappings);

        forkJoin(
            this.service.getDetectorNodeTemplates(),
            this.service.getDetectorRelationshipTemplates()
        ).subscribe(
            data => this.handleData(data),
            error => this.handleError(error)
        );

        // TODO: fill the dynamicTable
        // this.dynamicTableData = [
        //   new DynamicDropdownData<>()
        // ]
    }

    private handleError(error: any) {
        this.loading = false;
        this.notify.error(error.message);
    }

    save(): void {
        this.loading = true;
    }

    onChangeProperty() {
        this.save();
    }

    private handleData(data: [NodeTemplate[], RelationshipTemplate[]]) {
        this.detectorElements.concat(data[0]);
        this.detectorElements.concat(data[1]);
    }
}

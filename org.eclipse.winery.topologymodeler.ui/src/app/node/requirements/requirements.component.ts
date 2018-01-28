/********************************************************************************
 * Copyright (c) 2017 Contributors to the Eclipse Foundation
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
 ********************************************************************************/

import { Component,
    EventEmitter,
    Input,
    OnChanges,
    OnInit,
    Output,
    SimpleChanges
} from '@angular/core';

@Component({
    selector: 'winery-requirements',
    templateUrl: './requirements.component.html',
    styleUrls: ['./requirements.component.css']
})
/**
 * This Handles Information about the nodes requirements
 */
export class RequirementsComponent implements OnInit, OnChanges {
    @Output() toggleModalHandler: EventEmitter<any>;
    @Input() currentNodeData: any;
    requirements: any[] = [];
    requirementsExist: boolean;

    constructor () {
        this.toggleModalHandler = new EventEmitter();
    }

    /**
     * Angular lifecycle event.
     */
    ngOnChanges (changes: SimpleChanges) {
        if (changes.currentNodeData.currentValue.currentProperties) {
            this.requirements = changes.currentNodeData.currentValue.currentProperties.requirement;
            this.requirementsExist = true;
        }
    }

    /**
     * Propagates the click event to node.component, where requirements modal gets opened.
     * @param $event
     */
    public toggleModal ($event) {
        this.toggleModalHandler.emit(this.currentNodeData);
    }

    ngOnInit () {
    }

}

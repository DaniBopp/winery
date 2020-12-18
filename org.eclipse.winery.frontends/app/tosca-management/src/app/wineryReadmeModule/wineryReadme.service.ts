/*******************************************************************************
 * Copyright (c) 2017-2020 Contributors to the Eclipse Foundation
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
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { InstanceService } from '../instance/instance.service';
import { HttpClient, HttpHeaders, HttpResponse } from '@angular/common/http';
import { backendBaseURL } from '../configuration';
import { Router } from '@angular/router';

@Injectable()
export class ReadmeService {
    readonly path: string;
    constructor(private http: HttpClient,
                private sharedData: InstanceService,
                private route: Router) {
    }

    getData(): Observable<string> {
        const headers = new HttpHeaders({ 'Accept': 'text/plain' });
        return this.http.get(
            this.sharedData.path + '/README.md',
            { headers: headers, responseType: 'text' }
        );
    }

    save(readmeFile: String): Observable<HttpResponse<string>> {
        return this.http.put(
            this.sharedData.path + '/README.md',
            readmeFile,
            { observe: 'response', responseType: 'text' });
    }

    saveDocumentationData(documentationData: string): Observable<HttpResponse<string>> {
        return this.http.put(
            backendBaseURL + this.route.url + '/',
            documentationData,
            { observe: 'response', responseType: 'text' }
        );
    }
}

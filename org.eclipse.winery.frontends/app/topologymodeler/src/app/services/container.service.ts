/*******************************************************************************
 * Copyright (c) 2019 Contributors to the Eclipse Foundation
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
import { CsarUpload } from '../models/container/csar-upload.model';
import { of } from 'rxjs';
import { Observable } from 'rxjs/Rx';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { catchError, concatMap, filter, map } from 'rxjs/operators';
import { NodeTemplateInstanceStates, PlanTypes, ServiceTemplateInstanceStates } from '../models/enums';
import { Csar } from '../models/container/csar.model';
import { ServiceTemplate } from '../models/container/service-template.model';
import { PlanResources } from '../models/container/plan-resources.model';
import { PlanInstanceResources } from '../models/container/plan-instance-resources.model';
import { ServiceTemplateInstance } from '../models/container/service-template-instance';
import { ServiceTemplateInstanceResources } from '../models/container/service-template-instance-resources.model';
import { Plan } from '../models/container/plan.model';
import { NodeTemplateResources } from '../models/container/node-template-resources.model';
import { NodeTemplateInstanceResources } from '../models/container/node-template-instance-resources.model';
import { NodeTemplateInstance } from '../models/container/node-template-instance.model';
import { NgRedux } from '@angular-redux/store';
import { IWineryState } from '../redux/store/winery.store';
import { PlanInstance } from '../models/container/plan-instance.model';
import { PlanLogEntry } from '../models/container/plan-log-entry.model';
import { InputParameter } from '../models/container/input-parameter.model';
import { OutputParameter } from '../models/container/output-parameter.model';
import { NodeTemplate } from '../models/container/node-template.model';
import { AdaptationPayload } from '../models/container/adaptation-payload.model';

@Injectable()
export class ContainerService {
    private containerUrl: string;

    private readonly headerAcceptJSON = {
        headers: new HttpHeaders({
            'Accept': 'application/json'
        })
    };
    private readonly headerContentJSON = {
        headers: new HttpHeaders({
            'Content-Type': 'application/json'
        })
    };
    private readonly headerContentTextPlain = {
        headers: new HttpHeaders({
            'Content-Type': 'text/plain'
        })
    };

    private readonly baseInstallationPayload = [
        { 'name': 'instanceDataAPIUrl', 'type': 'String', 'required': 'YES' },
        { 'name': 'csarEntrypoint', 'type': 'String', 'required': 'YES' },
        { 'name': 'CorrelationID', 'type': 'String', 'required': 'YES' }
    ];
    private readonly baseTerminationPayload = [
        { 'name': 'instanceDataAPIUrl', 'type': 'String', 'required': 'YES' },
        { 'name': 'OpenTOSCAContainerAPIServiceInstanceURL', 'type': 'String', 'required': 'YES' },
        { 'name': 'CorrelationID', 'type': 'String', 'required': 'YES' }
    ];
    private readonly baseTransformationPayload = [
        { 'name': 'CorrelationID', 'type': 'String', 'required': 'YES' },
        { 'name': 'instanceDataAPIUrl', 'type': 'String', 'required': 'YES' },
        { 'name': 'planCallbackAddress_invoker', 'type': 'String', 'required': 'YES' },
        { 'name': 'csarEntrypoint', 'type': 'String', 'required': 'YES' },
        { 'name': 'OpenTOSCAContainerAPIServiceInstanceURL', 'type': 'String', 'required': 'YES' },
    ];
    private readonly hidden_input_parameters = [
        'CorrelationID',
        'csarID',
        'serviceTemplateID',
        'containerApiAddress',
        'instanceDataAPIUrl',
        'planCallbackAddress_invoker',
        'csarEntrypoint',
        'OpenTOSCAContainerAPIServiceInstanceID',
        'OpenTOSCAContainerAPIServiceInstanceURL'
    ];

    constructor(
        private ngRedux: NgRedux<IWineryState>,
        private http: HttpClient,
    ) {
        this.ngRedux.select(state => state.liveModelingState.containerUrl)
            .subscribe(containerUrl => {
                this.containerUrl = containerUrl;
            });
    }

    public installApplication(uploadPayload: CsarUpload): Observable<any> {
        return this.http.post(this.combineURLs(this.containerUrl, 'csars'), uploadPayload, this.headerContentJSON);
    }

    public isApplicationInstalled(csarId: string): Observable<boolean> {
        const csarUrl = this.combineURLs(this.combineURLs(this.containerUrl, 'csars'), csarId);
        return this.http.get(csarUrl, { observe: 'response' }).pipe(
            map(resp => resp.ok),
            catchError(() => of(false))
        );
    }

    public deleteApplication(csarId: string): Observable<any> {
        const url = this.combineURLs(this.combineURLs(this.containerUrl, 'csars'), csarId);
        return this.http.delete(url);
    }

    public getCsar(csarId: string): Observable<Csar> {
        const csarUrl = this.combineURLs(this.combineURLs(this.containerUrl, 'csars'), csarId);
        return this.http.get<Csar>(csarUrl, this.headerAcceptJSON);
    }

    private getServiceTemplate(csarId: string): Observable<ServiceTemplate> {
        return this.getCsar(csarId).pipe(
            concatMap(resp => this.http.get<ServiceTemplate>(resp._links['servicetemplate'].href, this.headerAcceptJSON))
        );
    }

    public deployServiceTemplateInstance(csarId: string, buildPlanInputParameters: InputParameter[]): Observable<string> {
        const payload = [...buildPlanInputParameters, ...this.baseInstallationPayload];

        return this.getBuildPlan(csarId).pipe(
            concatMap(resp => this.http.post(resp._links['instances'].href, payload, {
                headers: new HttpHeaders({
                    'Content-Type': 'application/json'
                }),
                responseType: 'text'
            }))
        );
    }

    public waitForServiceTemplateInstanceIdAfterDeployment(csarId: string, correlationId: string, interval: number, timeout: number): Observable<string> {
        return Observable.timer(0, interval)
            .concatMap(() => this.getServiceTemplateInstanceIdAfterDeployment(csarId, correlationId))
            .first(resp => resp !== '')
            .timeout(timeout);
    }

    public getServiceTemplateInstanceState(csarId: string, serviceTemplateInstanceId: string): Observable<ServiceTemplateInstanceStates> {
        return this.getServiceTemplateInstance(csarId, serviceTemplateInstanceId).pipe(
            map(resp => ServiceTemplateInstanceStates[resp.state]),
        );
    }

    private getServiceTemplateInstance(csarId: string, serviceTemplateInstanceId: string): Observable<ServiceTemplateInstance> {
        return this.getServiceTemplate(csarId).pipe(
            concatMap(resp => this.http.get<ServiceTemplateInstanceResources>(resp._links['instances'].href, this.headerAcceptJSON)),
            concatMap(resp => this.http.get<ServiceTemplateInstance>(
                resp.service_template_instances.find(instance =>
                    instance.id.toString() === serviceTemplateInstanceId)._links['self'].href, this.headerAcceptJSON)
            )
        );
    }

    private getServiceTemplateInstanceIdAfterDeployment(csarId: string, correlationId: string): Observable<string> {
        return this.getBuildPlanInstance(csarId, correlationId).pipe(
            map(resp => resp.service_template_instance_id.toString()),
            catchError(() => of(''))
        );
    }

    public waitForServiceTemplateInstanceInState(
        csarId: string,
        serviceTemplateInstanceId: string,
        state: ServiceTemplateInstanceStates,
        interval: number,
        timeout: number
    ): Observable<ServiceTemplateInstanceStates> {
        return Observable.timer(0, interval)
            .concatMap(() => this.isServiceTemplateInstanceInState(csarId, serviceTemplateInstanceId, state))
            .first(resp => resp != null)
            .timeout(timeout);
    }

    private isServiceTemplateInstanceInState(
        csarId: string,
        serviceTemplateInstanceId: string,
        state: ServiceTemplateInstanceStates
    ): Observable<ServiceTemplateInstanceStates> {
        return this.getServiceTemplateInstanceState(csarId, serviceTemplateInstanceId).pipe(
            filter(resp => resp === state || resp === ServiceTemplateInstanceStates.ERROR),
            catchError(() => of(null))
        );
    }

    public getNodeTemplateInstanceState(csarId: string, serviceTemplateInstanceId: string, nodeTemplateId: string): Observable<NodeTemplateInstanceStates> {
        return this.getNodeTemplateInstance(csarId, serviceTemplateInstanceId, nodeTemplateId).pipe(
            map(resp => NodeTemplateInstanceStates[resp.state])
        );
    }

    public getNodeTemplates(csarId: string, serviceTemplateInstanceId: string): Observable<Array<NodeTemplate>> {
        return this.getServiceTemplate(csarId).pipe(
            concatMap(resp => this.http.get<NodeTemplateResources>(resp._links['nodetemplates'].href, this.headerAcceptJSON)),
            map(resp => resp.node_templates)
        );
    }

    public getNodeTemplateInstance(csarId: string, serviceTemplateInstanceId: string, nodeTemplateId: string): Observable<NodeTemplateInstance> {
        return this.getNodeTemplates(csarId, serviceTemplateInstanceId).pipe(
            // concatMap(resp => this.http.get<NodeTemplateResources>(resp._links['nodetemplates'].href, this.headerAcceptJSON)),
            // concatMap(resp => this.http.get<NodeTemplate>(resp.node_templates.find(template => template.id === nodeTemplateId)._links['self'].href,
            // this.headerAcceptJSON)), concatMap(resp => this.http.get<NodeTemplateInstanceResources>(resp._links['instances'].href, this.headerAcceptJSON)),
            // map(resp => resp.node_template_instances.find(instance => instance.service_template_instance_id.toString() ===
            // this.currentServiceTemplateInstanceId))

            // TODO: temporary until parsing error fixed
            concatMap(resp => this.http.get<NodeTemplateInstanceResources>(
                resp.find(template => template.id.toString() === nodeTemplateId)._links['self'].href + '/instances', this.headerAcceptJSON)
            ), // todo temp
            concatMap(resp => this.http.get<NodeTemplateInstance>(
                resp.node_template_instances.find(instance =>
                    instance.service_template_instance_id.toString() === serviceTemplateInstanceId)._links['self'].href, this.headerAcceptJSON)
            )
        );
    }

    public terminateServiceTemplateInstance(csarId: string, serviceTemplateInstanceId: string): Observable<string> {
        return this.getTerminationPlan(csarId, serviceTemplateInstanceId).pipe(
            concatMap(resp => this.http.post(resp._links['instances'].href, this.baseTerminationPayload, {
                headers: new HttpHeaders({
                    'Content-Type': 'application/json'
                }),
                responseType: 'text'
            }))
        );
    }

    public generateTransformationPlan(sourceCsarId: string, targetCsarId: string): Observable<string> {
        const transformPayload = {
            'source_csar_name': sourceCsarId,
            'target_csar_name': targetCsarId
        };
        const transformationPlanId = this.stripCsarSuffix(sourceCsarId) + '_transformTo_' + this.stripCsarSuffix(targetCsarId) + '_plan';

        const endpoint = this.combineURLs(this.containerUrl, 'csars/transform');
        return this.http.post(endpoint, transformPayload, this.headerContentJSON).pipe(
            map(_ => transformationPlanId)
        );
    }

    public executeTransformationPlan(
        serviceTemplateInstanceId: string,
        sourceCsarId: string,
        targetCsarId: string,
        inputParameters: InputParameter[]
    ): Observable<string> {
        const planId = this.getTransformationPlanId(sourceCsarId, targetCsarId);
        const payload = [...inputParameters, ...this.baseTransformationPayload];

        return this.getManagementPlan(sourceCsarId, serviceTemplateInstanceId, planId).pipe(
            concatMap(resp => this.http.post(resp._links['instances'].href,
                payload,
                {
                    headers: new HttpHeaders({
                        'Content-Type': 'application/json'
                    }),
                    responseType: 'text'
                }))
        );
    }

    public waitForServiceTemplateInstanceIdAfterMigration(
        csarId: string,
        serviceTemplateInstanceId: string,
        correlationId: string,
        sourceCsarId: string,
        targetCsarId: string,
        interval: number,
        timeout: number
    ): Observable<string> {
        const planId = this.getTransformationPlanId(sourceCsarId, targetCsarId);
        return Observable.timer(0, interval)
            .concatMap(() => this.getServiceTemplateInstanceIdAfterMigration(csarId, serviceTemplateInstanceId, correlationId, planId))
            .first(resp => resp !== '')
            .timeout(timeout);
    }

    private getServiceTemplateInstanceIdAfterMigration(
        csarId: string,
        serviceTemplateInstanceId: string,
        correlationId: string,
        planId: string): Observable<string> {
        return this.getManagementPlans(csarId, serviceTemplateInstanceId).pipe(
            concatMap(resp => this.http.get<PlanInstanceResources>(
                resp.find(plan => plan.id === planId && plan.plan_type === PlanTypes.TransformationPlan)._links['instances'].href, this.headerAcceptJSON)
            ),
            map(resp => resp.plan_instances.find(
                plan => plan.correlation_id.toString() === correlationId).outputs.find(output => output.name === 'instanceId').value),
            catchError(() => of(''))
        );
    }

    public getTransformationPlanLogs(
        csarId: string,
        serviceTemplateInstanceId: string,
        correlationId: string,
        sourceCsarId: string,
        targetCsarId: string
    ): Observable<Array<PlanLogEntry>> {
        const planId = this.getTransformationPlanId(sourceCsarId, targetCsarId);
        return this.getManagementPlans(csarId, serviceTemplateInstanceId).pipe(
            concatMap(resp => this.http.get<PlanInstanceResources>(
                resp.find(plan => plan.id === planId && plan.plan_type === PlanTypes.TransformationPlan)._links['instances'].href, this.headerAcceptJSON)
            ),
            map(resp => resp.plan_instances.find(plan => plan.correlation_id.toString() === correlationId).logs)
        );
    }

    private getTransformationPlanId(sourceCsarId: string, targetCsarId: string): string {
        return this.stripCsarSuffix(sourceCsarId) + '_transformTo_' + this.stripCsarSuffix(targetCsarId) + '_plan';
    }

    public getBuildPlanLogs(csarId: string, correlationId: string): Observable<Array<PlanLogEntry>> {
        return this.getBuildPlanInstance(csarId, correlationId).pipe(
            map(resp => resp.logs)
        );
    }

    public getRequiredBuildPlanInputParameters(csarId: string): Observable<Array<InputParameter>> {
        return this.getAllBuildPlanInputParameters(csarId).pipe(
            map(resp => resp.filter(input => this.hidden_input_parameters.indexOf(input.name) === -1))
        );
    }

    public getAllBuildPlanInputParameters(csarId: string): Observable<Array<InputParameter>> {
        return this.getBuildPlan(csarId).pipe(
            map(resp => resp.input_parameters)
        );
    }

    public getBuildPlanOutputParameters(csarId: string): Observable<Array<OutputParameter>> {
        return this.getBuildPlan(csarId).pipe(
            map(resp => resp.output_parameters)
        );
    }

    private getBuildPlan(csarId: string): Observable<Plan> {
        return this.getServiceTemplate(csarId).pipe(
            concatMap(resp => this.http.get<PlanResources>(resp._links['buildplans'].href, this.headerAcceptJSON)),
            map(resp => resp.plans.find(plan => plan.plan_type === PlanTypes.BuildPlan))
        );
    }

    private getBuildPlanInstance(csarId: string, correlationId: string): Observable<PlanInstance> {
        return this.getBuildPlan(csarId).pipe(
            concatMap(resp => this.http.get<PlanInstanceResources>(resp._links['instances'].href, this.headerAcceptJSON)),
            map(resp => resp.plan_instances.find(planInstance => planInstance.correlation_id.toString() === correlationId)),
        );
    }

    public getServiceTemplateInstanceBuildPlanInstance(csarId: string, serviceTemplateInstanceId: string): Observable<PlanInstance> {
        return this.getServiceTemplateInstance(csarId, serviceTemplateInstanceId).pipe(
            concatMap(resp => this.http.get<PlanInstance>(resp._links['build_plan_instance'].href, this.headerAcceptJSON)),
            map(resp => {
                resp.inputs = resp.inputs.filter(input => this.hidden_input_parameters.indexOf(input.name) === -1);
                return resp;
            })
        );
    }

    private getManagementPlans(csarId: string, serviceTemplateInstanceId: string): Observable<Array<Plan>> {
        return this.getServiceTemplateInstance(csarId, serviceTemplateInstanceId).pipe(
            concatMap(resp => this.http.get<PlanResources>(resp._links['managementplans'].href, this.headerAcceptJSON)),
            map(resp => resp.plans)
        );
    }

    private getManagementPlan(csarId: string, serviceTemplateInstanceId: string, planId: string): Observable<Plan> {
        return this.getManagementPlans(csarId, serviceTemplateInstanceId).pipe(
            map(resp => resp.find(plan => plan.id.toString() === planId))
        );
    }

    public getManagementPlanInputParameters(csarId: string, serviceTemplateInstanceId: string, planId: string): Observable<InputParameter[]> {
        return this.getManagementPlan(csarId, serviceTemplateInstanceId, planId).pipe(
            map(resp => resp.input_parameters.filter(input => this.hidden_input_parameters.indexOf(input.name) === -1))
        );
    }

    private getTerminationPlan(csarId: string, serviceTemplateInstanceId: string): Observable<Plan> {
        return this.getManagementPlans(csarId, serviceTemplateInstanceId).pipe(
            map(resp => resp.find(plan => plan.plan_type === PlanTypes.TerminationPlan))
        );
    }

    public generateAdaptationPlan(csarId: string, serviceTemplateInstanceId: string, payload: AdaptationPayload) {
        // todo
    }

    public updateNodeTemplateInstanceState(
        csarId: string,
        serviceTemplateInstanceId: string,
        nodeTemplateId: string,
        state: NodeTemplateInstanceStates
    ): Observable<any> {
        return this.getNodeTemplateInstance(csarId, serviceTemplateInstanceId, nodeTemplateId).pipe(
            concatMap(resp => this.http.put(resp._links['state'].href, state.toString(), this.headerContentTextPlain))
        );
    }

    private combineURLs(baseURL: string, relativeURL: string) {
        return relativeURL
            ? baseURL.replace(/\/+$/, '') + '/' + relativeURL.replace(/^\/+/, '')
            : baseURL;
    }

    private stripCsarSuffix(csarId: string) {
        const csarEnding = '.csar';
        return csarId.endsWith(csarEnding) ? csarId.slice(0, -csarEnding.length) : csarId;
    }
}

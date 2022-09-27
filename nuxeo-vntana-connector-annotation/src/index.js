import './styles.css';
import Nuxeo from 'nuxeo';

const nuxeo = new Nuxeo({
    baseURL: '/nuxeo'
});

let nxUser;
let allUsers;
let nxDoc;
let nxAnnotations;
let nxComments;
let vnAnnotations;
let docId;
let blobUrl;
let thumbnailUrl;

window.addEventListener("load", function () {
    const params = new URLSearchParams(window.location.search);
    docId = params.get('docId');
    blobUrl = params.get('blobUrl');
    thumbnailUrl = params.get('thumbnailUrl');
    getContext().then(() => {
        setupViewer();
    });
});

function getContext() {
    //get current user
    return nuxeo.request('me').get().then(user => {
        nxUser = user;
        //get current document
        return nuxeo.repository().enricher('document','thumbnail').fetch(docId, {
            schemas: ['dublincore', 'file']
        });
    }).then(doc => {
        nxDoc = doc;
        //set glb blob url
        if (!blobUrl) {
            blobUrl = nxDoc.get('file:content').data;
        }
        //set thumbnail url
        if (!thumbnailUrl) {
            thumbnailUrl = nxDoc.contextParameters.thumbnail.url;
        }

        //get annotations
        return nuxeo.request(`id/${docId}/@annotation`).get({
            queryParams: {
                xpath: "file:content"
            }
        });
    }).then(annotations => {
        nxAnnotations = annotations.entries;
        //get comments
        if (nxAnnotations.length > 0) {
            return nuxeo.request(`id/${nxDoc.uid}/@annotation/comments`).post({
                body: annotations.entries.map((annotation) => {return annotation.id})
            });
        } else {
            //no annotation so no need to try to fetch annotation comment
            return Promise.resolve({ entries: []});
        }
    }).then(comments => {
        nxComments = comments.entries;
        vnAnnotations = nx2vntanaAnnotations(nxAnnotations);
        allUsers = buildUserList();
        return Promise.resolve();
    }).catch(function () {
        displayErrorMessage();
    });
}

function setupViewer() {

    let viewerEl = document.querySelector('#viewer');

    window.vnvieweditor.createViewerEditor(viewerEl, {
        fileGLTF: blobUrl,
        thumbnailUrl: thumbnailUrl,
        panelOrder: ["annotation", "settings"],
        hideSaveBtn: true,
        currentUserId: nxUser.id,
        usersList: allUsers,
        getAnnotations: () => {return vnAnnotations;},
        createAnnotation: createAnnotation,
        updateAnnotation: updateAnnotation,
        deleteAnnotation: deleteAnnotation,
        resolveAnnotation: resolveAnnotation,
        getAnnotationComments: getAnnotationComments,
        createComment: createComment,
        updateComment: updateComment,
        deleteComment: deleteComment,
        onError: () => {
            displayErrorMessage();
        },
        onSuccess: () => {
            let menuElement = document.querySelector('#ViewerEditorBtn');
            menuElement.click();

            let panels = document.querySelectorAll('.ViewerEditorNavigation div');
            panels[1].click();

            let navigationElement = document.querySelector('.ViewerEditorNavigation');
            if (navigationElement) {
                navigationElement.style.display = "none";
            }
        }
    });
}

function nx2vntanaAnnotations(nxAnnotations) {
    let vnAnnotations = nxAnnotations.map((entry,index) => {
        let vnAnnotation = JSON.parse(entry.entity);
        return {
            userId: entry.author,
            dimensions: vnAnnotation.dimensions,
            resolved: vnAnnotation.resolved,
            number: index+1,
            text: entry.text,
            uuid: entry.id,
            created: entry.creationDate.slice(0, -1),
            updated: entry.modificationDate.slice(0, -1),
            commentsCount: getAnnotationComments({entityUuid:entry.id}).response.comments.total
        }
    });
    return {
        response: {
            annotations: {
                grid: vnAnnotations,
                totalCount: vnAnnotations.length,
                nextNumber: vnAnnotations.length + 1
            }
        }
    }
}

function buildUserList() {
    let annotationUsers = nxAnnotations.map(entry => entry.author);
    let commentUsers = nxComments.map(entry => entry.author);
    return Array.from(new Set(annotationUsers.concat(commentUsers))).map(entry => {
        return {
            userId: entry,
            userName: entry === nxUser.id ? `${nxUser.properties.firstName} ${nxUser.properties.lastName}` : entry
        };
    });
}

function createAnnotation(requestData) {
    return nuxeo.request(`id/${nxDoc.uid}/@annotation`).post({
        body: {
            "entity-type": "annotation",
            xpath: "file:content",
            author: nxUser.id,
            text: requestData.text,
            origin: "vntanaViewer",
            entity: JSON.stringify(requestData)
        }
    }).then(annotation => {
        return {
            userId: annotation.author,
            dimensions: JSON.parse(annotation.entity).dimensions,
            number: requestData.number,
            text: annotation.text,
            uuid: annotation.id
        }
    });
}

function updateAnnotation(requestData) {
    return nuxeo.request(`id/${nxDoc.uid}/@annotation/${requestData.uuid}`).put({
        body: {
            "entity-type": "annotation",
            xpath: "file:content",
            text: requestData.text,
            origin: "vntanaViewer",
            entity: JSON.stringify(requestData)
        }
    }).then(annotation => {
        return {
            userId: annotation.author,
            dimensions: JSON.parse(annotation.entity).dimensions,
            number: requestData.number,
            text: annotation.text,
            uuid: annotation.id
        }
    });
}

function resolveAnnotation(requestData) {
    return nuxeo.request(`id/${nxDoc.uid}/@annotation/${requestData.uuid}`).get().then(annotation => {
        let entity = JSON.parse(annotation.entity);
        entity.resolved = requestData.resolved;
        return nuxeo.request(`id/${nxDoc.uid}/@annotation/${requestData.uuid}`).put({
            body: {
                "entity-type": "annotation",
                xpath: "file:content",
                entity: JSON.stringify(entity)
            }
        })
    });
}

function deleteAnnotation(annotation) {
    return nuxeo.request(`id/${nxDoc.uid}/@annotation/${annotation.uuid}`).delete();
}

function getAnnotationComments(annotation) {
    let vnComments = nxComments.flatMap(entry => {
        if (entry.parentId === annotation.entityUuid) {
            return [{
                userId: entry.author,
                "entityType": "ANNOTATION",
                message: entry.text,
                uuid: entry.id,
                entityUuid: annotation.entityUuid,
                created: entry.creationDate.slice(0, -1),
                updated: entry.modificationDate.slice(0, -1)
            }];
        } else {
            return [];
        }
    });
    return {
        response: {
            comments: {
                grid: vnComments,
                total: vnComments.length,
            }
        }
    }
}

function createComment(requestData) {
    nuxeo.request(`id/${requestData.entityUuid}/@comment`).post({
        body: {
            "entity-type": "comment",
            text: requestData.message,
            origin: "vntanaViewer"
        }
    });
    return {
        entityUuid: requestData.entityUuid,
        uuid: 'tmp1'
    };
}

function updateComment(requestData) {
    let uuid = requestData.uuid;
    return nuxeo.request(`id/${requestData.entityUuid}/@comment/${uuid}`).put({
        body: {
            "entity-type": "comment",
            text: requestData.message
        }
    });
}

function deleteComment(uuid) {
    return nuxeo.request(`id/${uuid}`).delete();
}

function displayErrorMessage() {
    let messageElement = document.querySelector('#message');
    messageElement.style.display = 'block';
    let errorElement = document.querySelector('#error');
    errorElement.textContent = 'Bummer. An error happened while loading the model.'
}


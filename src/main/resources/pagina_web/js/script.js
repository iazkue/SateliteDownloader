console.log('script.js is loading...');

var map = L.map('map').setView([42.81687, -1.64323], 7);

//Create different layers and add to map default

var streets = L.tileLayer('http://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
  minZoom: 0,
  maxZoom: 17,
  attribution:
    '&copy; <a href="http://www.openstreetmap.org/copyright">OpenStreetMap</a>',
});
map.addLayer(streets);

var real = L.tileLayer(
  'http://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}',
  {
    minZoom: 0,
    maxZoom: 17,
    attribution:
      'Tiles &copy; Esri &mdash; Source: Esri, i-cubed, USDA, USGS, AEX, GeoEye, Getmapping, Aerogrid, IGN, IGP, UPR-EGP, and the GIS User Community',
  },
);

var baseMaps = {
  Streets: streets,
  Satellite: real,
};

//var control = L.control.layers(baseMaps, overlayMaps).addTo(map);
var overlays = {
  //add any overlays here
};
L.control.layers(baseMaps, overlays, { position: 'bottomleft' }).addTo(map);

// featureGroup to control layer for selected area
var featureGroup = L.featureGroup().addTo(map);
// Draw controls
var drawControl = new L.Control.Draw({
  edit: {
    featureGroup: featureGroup,
  },
  draw: {
    polygon: true,
    polyline: false,
    rectangle: true,
    circle: false,
    marker: false,
  },
}).addTo(map);

// Area selection
var areaOfInterest;
var textInfo;
var points;
// GeoJason object with the areaOfInterestInformation
var geoJasonArea;
var area = 0;
var max_area = 50000;
var msg;
var areaSh = L.control.layers;

map.on('draw:created', showPolygonArea);
map.on('draw:edited', showPolygonAreaEdited);
map.on('draw:deleted', function () {
  document.getElementById('post-btn').disabled = true;
  document.getElementById('preview-btn').disabled = true;
});

function showPolygonAreaEdited(e) {
  e.layers.eachLayer(function (layer) {
    showPolygonArea({ layer: layer });
  });
}

function showPolygonArea(e) {
  map.removeLayer(areaSh);
  featureGroup.clearLayers();
  featureGroup.addLayer(e.layer);
  areaOfInterest = e.layer;
  geoJasonArea = areaOfInterest.toGeoJSON();

  points = '';
  i = 1;
  for (point of e.layer._latlngs[0]) {
    points =
      points +
      i +
      ': (' +
      point.lat.toFixed(4) +
      ', ' +
      point.lng.toFixed(4) +
      ')\n';
    i++;
  }

  // specify popup options for too large areas
  var customOptions = {
    maxWidth: '500',
    className: 'custom',
  };

  area = LGeo.area(e.layer) / 1000000;

  var customPopup =
    'Too large area!' +
    '<dt>' +
    area.toFixed(2) +
    ' km<sup>2</sup>' +
    '</dt>' +
    '<dt>Max allowed: 50000 km<sup>2</sup></dt>';

  if (area < max_area) {
    e.layer.bindPopup(area.toFixed(2) + ' km<sup>2</sup>');
    document.getElementById('post-btn').disabled = true;
    document.getElementById('preview-btn').disabled = false;
  } else {
    e.layer.bindPopup(customPopup, customOptions);
    document.getElementById('post-btn').disabled = true;
    document.getElementById('preview-btn').disabled = true;
  }
  //e.layer.openPopup();
  setTimeout(() => {
    e.layer.openPopup();
  }, 100);
}

// Predefined area (province)
var province;
const prov_from = document.getElementById('provinces');
prov_from.addEventListener('change', function () {
  province = this.value;
});

// Date selection
var initDay;
const day_from = document.getElementById('iday');
console.log('Date input iday:', day_from);
day_from.addEventListener('change', function () {
  initDay = this.value;
  console.log('Initial date changed to:', initDay);
});

var endDay;
const day_until = document.getElementById('fday');
console.log('Date input fday:', day_until);
day_until.addEventListener('change', function () {
  endDay = this.value;
  console.log('Final date changed to:', endDay);
});

//Cloud cover selection
var cloudCover = 0;
var slide = document.getElementById('idSlider');
var y = document.getElementById('f');
/*y.innerHTML = slide.value;

#slide.oninput = function () {
    y.innerHTML = this.value;
    cloudCover = this.value;
}*/

// Download Queue Management
let queuePollInterval = null;

function startQueuePolling() {
  if (!queuePollInterval) {
    queuePollInterval = setInterval(fetchAndUpdateQueue, 1000);
  }
}

async function fetchAndUpdateQueue() {
  try {
    const response = await fetch('/api/downloadQueue');
    if (!response.ok) return;
    const tasks = await response.json();
    renderDownloadQueue(tasks);
  } catch (e) {
    console.error('Error fetching download queue:', e);
  }
}

function renderDownloadQueue(tasks) {
  const queueSection = document.getElementById('download-queue-section');
  const queueList = document.getElementById('queue-task-list');
  const queueBadge = document.getElementById('queue-badge');

  if (!queueSection || !queueList) return;

  queueSection.style.display = 'block';

  if (!tasks || tasks.length === 0) {
    if (queueBadge) {
      queueBadge.textContent = '0 activas';
    }
    queueList.innerHTML = '<p class="text-muted text-center m-0 py-2" style="font-size: 0.85rem;">No hay descargas en la cola</p>';
    return;
  }
  const activeTasks = tasks.filter(t => t.status === 'QUEUED' || t.status === 'DOWNLOADING').length;
  if (queueBadge) {
    queueBadge.textContent = `${activeTasks} activa(s)`;
  }

  // Sort tasks by taskId descending so newest is at top
  const sortedTasks = [...tasks].sort((a, b) => b.taskId.localeCompare(a.taskId));

  queueList.innerHTML = sortedTasks.map(task => {
    let badgeClass = 'bg-secondary';
    let badgeText = task.status;
    let progressBarClass = 'bg-primary';

    if (task.status === 'QUEUED') {
      badgeClass = 'bg-warning text-dark';
      badgeText = 'En Cola';
      progressBarClass = 'bg-warning';
    } else if (task.status === 'DOWNLOADING') {
      badgeClass = 'bg-primary';
      badgeText = 'Descargando';
      progressBarClass = 'bg-primary progress-bar-striped progress-bar-animated';
    } else if (task.status === 'COMPLETED') {
      badgeClass = 'bg-success';
      badgeText = 'Completado';
      progressBarClass = 'bg-success';
    } else if (task.status === 'CANCELLED') {
      badgeClass = 'bg-danger';
      badgeText = 'Cancelado';
      progressBarClass = 'bg-danger';
    } else if (task.status === 'FAILED') {
      badgeClass = 'bg-dark';
      badgeText = 'Fallido';
      progressBarClass = 'bg-dark';
    }

    const showCancel = task.status === 'QUEUED' || task.status === 'DOWNLOADING';
    const percent = task.percent || 0;
    const message = task.message || '';

    return `
      <div class="card p-2 border shadow-sm">
        <div class="d-flex justify-content-between align-items-center mb-1">
          <span class="fw-bold text-truncate" style="font-size: 0.85rem; max-width: 60%;">${task.taskId}</span>
          <span class="badge ${badgeClass}">${badgeText}</span>
        </div>
        <div class="progress mb-1" style="height: 18px;">
          <div class="progress-bar ${progressBarClass}" role="progressbar" style="width: ${percent}%;">${percent}%</div>
        </div>
        <div class="d-flex justify-content-between align-items-center mt-1">
          <small class="text-muted text-truncate font-monospace" style="font-size: 0.75rem; max-width: 70%;">${message}</small>
          ${showCancel ? `<button type="button" class="btn btn-sm btn-outline-danger py-0 px-2" style="font-size: 0.75rem;" onclick="cancelQueueTask('${task.taskId}')">Cancelar</button>` : ''}
        </div>
      </div>
    `;
  }).join('');
}

async function cancelQueueTask(taskId) {
  try {
    await fetch(`/api/cancelDownload/${taskId}`, { method: 'POST' });
    fetchAndUpdateQueue();
  } catch (e) {
    console.error('Error cancelling task:', e);
  }
}
window.cancelQueueTask = cancelQueueTask;

// Start queue polling immediately
startQueuePolling();

// Submit button controls
let currentPreviewController = null;

const cancelPreviewBtn = document.getElementById('cancel-preview-btn');
if (cancelPreviewBtn) {
  cancelPreviewBtn.addEventListener('click', () => {
    if (currentPreviewController) {
      currentPreviewController.abort();
    }
  });
}

const button_submit = document.getElementById('post-btn');
if (button_submit) {
  button_submit.addEventListener('click', async (event) => {
    event.preventDefault();

    if (area >= max_area) {
      alert('The selected area is too large\nPlease select an area under 50000 km\u00B2');
      return;
    }

    if (!initDay || !endDay || !geoJasonArea) {
      alert("Please select both a date range and a region of interest.");
      return;
    }

    const selectedImages = [];
    const checkboxes = document.querySelectorAll('.preview-checkbox');
    if (checkboxes.length > 0) {
      checkboxes.forEach(cb => {
        if (cb.checked) {
          selectedImages.push(cb.value);
        }
      });
    }

    const requestData = {
      iday: initDay,
      fday: endDay,
      geojson: JSON.stringify(geoJasonArea),
      selectedImages: selectedImages
    };

    try {
      const response = await fetch('/api/downloadImages', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(requestData)
      });

      if (response.ok) {
        const task = await response.json();
        alert('Added to download queue: ' + (task.taskId || 'Task started'));
        startQueuePolling();
        fetchAndUpdateQueue();
      } else {
        const errJson = await response.json();
        alert('Error starting download: ' + (errJson.message || 'Server error'));
      }
    } catch (err) {
      console.error('Error submitting download request:', err);
      alert('Error submitting download request: ' + err.message);
    }
  });
}

// Preview button controls
const button_preview = document.getElementById('preview-btn');
if (button_preview) {
  button_preview.addEventListener('click', async (event) => {
    event.preventDefault();

    if (area >= max_area) {
      alert('The selected area is too large\nPlease select an area under 50000 km\u00B2');
      return;
    }

    if (!initDay || !endDay || !geoJasonArea) {
      alert("Please select both a date range and a region of interest.");
      return;
    }

    const requestData = {
      iday: initDay,
      fday: endDay,
      geojson: JSON.stringify(geoJasonArea)
    };

    currentPreviewController = new AbortController();
    button_preview.classList.add('d-none');

    const previewUi = document.getElementById('preview-progress-ui');
    const progressBar = document.getElementById('preview-progress-bar');
    const progressText = document.getElementById('preview-progress-text');

    previewUi.classList.remove('d-none');
    progressBar.style.width = '0%';
    progressBar.textContent = '0%';
    progressText.textContent = 'Starting to search...';

    let lastCompletedResult = null;

    try {
      const response = await fetch('/api/downloadPreviews', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(requestData),
        signal: currentPreviewController.signal
      });

      if (!response.ok) {
        throw new Error('Error en el servidor: ' + response.statusText);
      }

      const reader = response.body.getReader();
      const decoder = new TextDecoder();
      let buffer = '';

      while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        buffer += decoder.decode(value, { stream: true });
        const lines = buffer.split('\n');
        buffer = lines.pop(); // save remainder

        for (const line of lines) {
          if (!line.trim()) continue;
          try {
            const data = JSON.parse(line);
            if (data.status === 'searching' || data.status === 'found') {
              progressText.textContent = data.message;
            } else if (data.status === 'progress') {
              progressBar.style.width = data.percent + '%';
              progressBar.textContent = data.percent + '%';
              progressText.textContent = data.message;
            } else if (data.status === 'completed') {
              progressBar.style.width = '100%';
              progressBar.textContent = '100%';
              progressText.textContent = 'Vistas previas cargadas';
              lastCompletedResult = data;
            } else if (data.status === 'error') {
              throw new Error(data.message);
            }
          } catch (err) {
            console.error('Error parsing stream chunk:', err, line);
          }
        }
      }
    } catch (err) {
      if (err.name === 'AbortError') {
        console.log('Preview request cancelled');
        progressText.textContent = 'Preview cancelado.';
      } else {
        console.error('Error in preview stream:', err);
        alert('Error descargando preview: ' + err.message);
      }
    } finally {
      button_preview.classList.remove('d-none');
      previewUi.classList.add('d-none');
    }

    if (lastCompletedResult) {
      displayPreviews(lastCompletedResult);
    }
  });
}

function displayPreviews(result) {
  const previewContainer = document.getElementById('preview-container');
  previewContainer.style.display = 'block';
  previewContainer.innerHTML = '<h5 class="mb-3">Select images to download:</h5><div class="row g-3"></div>';
  const gallery = previewContainer.querySelector('.row');

  if (result.previewImages && result.previewImages.length > 0) {
    result.previewImages.forEach(filename => {
      const colDiv = document.createElement('div');
      colDiv.className = 'col-6';
      colDiv.innerHTML = `
        <div class="card p-2 h-100 border-primary">
          <img src="/api/previews/${filename}" alt="${filename}" style="width: 100%; height: 150px; object-fit: cover;" class="mb-2 rounded">
          <div class="form-check text-start">
              <input class="form-check-input preview-checkbox" type="checkbox" value="${filename}" id="check_${filename}">
              <label class="form-check-label text-break" style="font-size: 0.8rem;" for="check_${filename}">
                  ${filename}
              </label>
          </div>
        </div>
      `;
      gallery.appendChild(colDiv);
    });

    const checkboxes = document.querySelectorAll('.preview-checkbox');
    checkboxes.forEach(cb => {
      cb.addEventListener('change', () => {
        const anyChecked = Array.from(checkboxes).some(c => c.checked);
        document.getElementById('post-btn').disabled = !anyChecked;
      });
    });
    document.getElementById('post-btn').disabled = true;
  } else {
    previewContainer.innerHTML += '<p class="text-muted">No se encontraron imágenes de vista previa.</p>';
    document.getElementById('post-btn').disabled = false;
  }
}

// Form submit handler (not needed since we handle it via button click)
$('#parameters').submit(function (eventObj) {
  eventObj.preventDefault(); // Prevent default form submission
  return false;
});

// Show areas
document.getElementById('sh-navarre').onclick = function () {
  featureGroup.clearLayers();
  map.removeLayer(areaSh);
  areaSh = omnivore.kml('media/navarra.kml');
  areaSh.addTo(map);
  $.ajax('media/navarra.kml', { dataType: 'text' })
    .done(function (xmlText) {
      const parser = new DOMParser();
      const xmlDoc = parser.parseFromString(xmlText, 'text/xml');
      const featureCollection = toGeoJSON.kml(xmlDoc);
      // Extract first feature from collection
      let feature =
        featureCollection.features && featureCollection.features.length > 0
          ? featureCollection.features[0]
          : featureCollection;

      // If it's a GeometryCollection, extract the first Polygon or MultiPolygon
      if (feature.geometry && feature.geometry.type === 'GeometryCollection') {
        const polygon = feature.geometry.geometries.find(
          (g) => g.type === 'Polygon' || g.type === 'MultiPolygon',
        );
        if (polygon) {
          feature = {
            type: 'Feature',
            properties: feature.properties || {},
            geometry: polygon,
          };
        }
      }

      geoJasonArea = feature;
      console.log('Navarre GeoJSON loaded:', JSON.stringify(geoJasonArea));
    })
    .fail(function (err) {
      console.error('Failed to load Navarre KML:', err);
    });
  document.getElementById('post-btn').disabled = true;
  document.getElementById('preview-btn').disabled = false;
};
document.getElementById('sh-rioja').onclick = function () {
  featureGroup.clearLayers();
  map.removeLayer(areaSh);
  areaSh = omnivore.kml('media/larioja.kml');
  areaSh.addTo(map);
  $.ajax('media/larioja.kml', { dataType: 'text' })
    .done(function (xmlText) {
      const parser = new DOMParser();
      const xmlDoc = parser.parseFromString(xmlText, 'text/xml');
      const featureCollection = toGeoJSON.kml(xmlDoc);
      // Extract first feature from collection
      let feature =
        featureCollection.features && featureCollection.features.length > 0
          ? featureCollection.features[0]
          : featureCollection;

      // If it's a GeometryCollection, extract the first Polygon or MultiPolygon
      if (feature.geometry && feature.geometry.type === 'GeometryCollection') {
        const polygon = feature.geometry.geometries.find(
          (g) => g.type === 'Polygon' || g.type === 'MultiPolygon',
        );
        if (polygon) {
          feature = {
            type: 'Feature',
            properties: feature.properties || {},
            geometry: polygon,
          };
        }
      }

      geoJasonArea = feature;
      console.log('La Rioja GeoJSON loaded:', JSON.stringify(geoJasonArea));
    })
    .fail(function (err) {
      console.error('Failed to load La Rioja KML:', err);
    });
  document.getElementById('post-btn').disabled = true;
  document.getElementById('preview-btn').disabled = false;
};
document.getElementById('sh-madrid').onclick = function () {
  featureGroup.clearLayers();
  map.removeLayer(areaSh);
  areaSh = omnivore.kml('media/madrid.kml');
  areaSh.addTo(map);
  $.ajax('media/madrid.kml', { dataType: 'text' })
    .done(function (xmlText) {
      const parser = new DOMParser();
      const xmlDoc = parser.parseFromString(xmlText, 'text/xml');
      const featureCollection = toGeoJSON.kml(xmlDoc);
      // Extract first feature from collection
      let feature =
        featureCollection.features && featureCollection.features.length > 0
          ? featureCollection.features[0]
          : featureCollection;

      // If it's a GeometryCollection, extract the first Polygon or MultiPolygon
      if (feature.geometry && feature.geometry.type === 'GeometryCollection') {
        const polygon = feature.geometry.geometries.find(
          (g) => g.type === 'Polygon' || g.type === 'MultiPolygon',
        );
        if (polygon) {
          feature = {
            type: 'Feature',
            properties: feature.properties || {},
            geometry: polygon,
          };
        }
      }

      geoJasonArea = feature;
      console.log('Madrid GeoJSON loaded:', JSON.stringify(geoJasonArea));
    })
    .fail(function (err) {
      console.error('Failed to load Madrid KML:', err);
    });
  document.getElementById('post-btn').disabled = true;
  document.getElementById('preview-btn').disabled = false;
};
document.getElementById('sh-euskadi').onclick = function () {
  featureGroup.clearLayers();
  map.removeLayer(areaSh);
  areaSh = omnivore.kml('media/euskadi.kml');
  areaSh.addTo(map);
  $.ajax('media/euskadi.kml', { dataType: 'text' })
    .done(function (xmlText) {
      const parser = new DOMParser();
      const xmlDoc = parser.parseFromString(xmlText, 'text/xml');
      const featureCollection = toGeoJSON.kml(xmlDoc);
      // Extract first feature from collection
      let feature =
        featureCollection.features && featureCollection.features.length > 0
          ? featureCollection.features[0]
          : featureCollection;

      // If it's a GeometryCollection, extract the first Polygon or MultiPolygon
      if (feature.geometry && feature.geometry.type === 'GeometryCollection') {
        const polygon = feature.geometry.geometries.find(
          (g) => g.type === 'Polygon' || g.type === 'MultiPolygon',
        );
        if (polygon) {
          feature = {
            type: 'Feature',
            properties: feature.properties || {},
            geometry: polygon,
          };
        }
      }

      geoJasonArea = feature;
      console.log('Euskadi GeoJSON loaded:', JSON.stringify(geoJasonArea));
    })
    .fail(function (err) {
      console.error('Failed to load Euskadi KML:', err);
    });
  document.getElementById('post-btn').disabled = true;
  document.getElementById('preview-btn').disabled = false;
};
document.getElementById('sh-aragon').onclick = function () {
  featureGroup.clearLayers();
  map.removeLayer(areaSh);
  areaSh = omnivore.kml('media/aragon.kml');
  areaSh.addTo(map);
  $.ajax('media/aragon.kml', { dataType: 'text' })
    .done(function (xmlText) {
      const parser = new DOMParser();
      const xmlDoc = parser.parseFromString(xmlText, 'text/xml');
      const featureCollection = toGeoJSON.kml(xmlDoc);
      // Extract first feature from collection
      let feature =
        featureCollection.features && featureCollection.features.length > 0
          ? featureCollection.features[0]
          : featureCollection;

      // If it's a GeometryCollection, extract the first Polygon or MultiPolygon
      if (feature.geometry && feature.geometry.type === 'GeometryCollection') {
        const polygon = feature.geometry.geometries.find(
          (g) => g.type === 'Polygon' || g.type === 'MultiPolygon',
        );
        if (polygon) {
          feature = {
            type: 'Feature',
            properties: feature.properties || {},
            geometry: polygon,
          };
        }
      }

      geoJasonArea = feature;
      console.log('Aragón GeoJSON loaded:', JSON.stringify(geoJasonArea));
    })
    .fail(function (err) {
      console.error('Failed to load Aragón KML:', err);
    });
  document.getElementById('post-btn').disabled = true;
  document.getElementById('preview-btn').disabled = false;
};
// Clear areas
document.getElementById('none').onclick = function () {
  featureGroup.clearLayers();
  map.removeLayer(areaSh);
  geoJasonArea = '';
  document.getElementById('post-btn').disabled = true;
  document.getElementById('preview-btn').disabled = true;
};

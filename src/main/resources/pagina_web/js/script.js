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

//Submit button controls
const button_submit = document.getElementById('post-btn');
console.log('Button element found:', button_submit);

if (button_submit) {
  console.log('Adding click listener to submit button');
  button_submit.addEventListener('click', async (event) => {
    event.preventDefault(); // Prevent default form submission

    console.log('Submit button clicked!');
    console.log('initDay:', initDay);
    console.log('endDay:', endDay);
    console.log('geoJasonArea:', geoJasonArea);
    console.log('area:', area);

    try {
      // Check if area is too large
      if (area >= max_area) {
        msg =
          'The selected area is too large\n' +
          'Please select an area under 50000 km\u00B2';
        window.alert(msg);
        return;
      }

      // Validate inputs
      if (!initDay || !endDay || !geoJasonArea) {
        alert("Please select both a date range and a region of interest.");
        return;
      }

      // Prepare the data to send (matching backend field names)
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
        geojson: JSON.stringify(geoJasonArea), // Convert object to string
        selectedImages: selectedImages
      };

      console.log('Sending request to /api/downloadImages:', requestData);

      // Send POST request to the download endpoint
      const response = await fetch('/api/downloadImages', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(requestData),
      });

      console.log('Response status:', response.status);
      console.log('Response headers:', response.headers.get('content-type'));

      // Get the response text first
      const responseText = await response.text();
      console.log('Response text:', responseText);

      // Try to parse as JSON
      let result;
      try {
        result = JSON.parse(responseText);
      } catch (e) {
        console.error('Failed to parse response as JSON:', e);
        alert(
          'Server error: Received non-JSON response\n' +
          responseText.substring(0, 200),
        );
        return;
      }

      if (response.ok) {
        console.log('Download completed successfully!', result);
        alert(
          'Download request submitted successfully!\n' +
          (result.message || 'Processing...'),
        );
      } else {
        console.error('Download failed:', result);
        alert('Download failed: ' + (result.message || 'Unknown error'));
      }
    } catch (err) {
      console.error(`Error: ${err}`);
      alert('Error submitting download request: ' + err.message);
    }

    // Clear the selection after submission
    document.getElementById('post-btn').disabled = true;
    featureGroup.clearLayers();
  });
} else {
  console.error('Submit button not found!');
}

// Preview button controls
const button_preview = document.getElementById('preview-btn');
console.log('Preview button element found:', button_preview);

if (button_preview) {
  console.log('Adding click listener to preview button');
  button_preview.addEventListener('click', async (event) => {
    event.preventDefault(); // Prevent default form submission

    console.log('Preview button clicked!');
    console.log('initDay:', initDay);
    console.log('endDay:', endDay);
    console.log('geoJasonArea:', geoJasonArea);
    console.log('area:', area);

    try {
      // Check if area is too large
      if (area >= max_area) {
        msg =
          'The selected area is too large\n' +
          'Please select an area under 50000 km\u00B2';
        window.alert(msg);
        return;
      }

      // Validate inputs
      if (!initDay || !endDay || !geoJasonArea) {
        alert("Please select both a date range and a region of interest.");
        return;
      }

      // Prepare the data to send (matching backend field names)
      const requestData = {
        iday: initDay,
        fday: endDay,
        geojson: JSON.stringify(geoJasonArea), // Convert object to string
      };

      console.log('Sending request to /api/downloadPreviews:', requestData);

      // Send POST request to the preview download endpoint
      const response = await fetch('/api/downloadPreviews', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(requestData),
      });

      console.log('Response status:', response.status);
      console.log('Response headers:', response.headers.get('content-type'));

      // Get the response text first
      const responseText = await response.text();
      console.log('Response text:', responseText);

      // Try to parse as JSON
      let result;
      try {
        result = JSON.parse(responseText);
      } catch (e) {
        console.error('Failed to parse response as JSON:', e);
        alert(
          'Server error: Received non-JSON response\n' +
          responseText.substring(0, 200),
        );
        return;
      }

      if (response.ok) {
        console.log('Preview download completed successfully!', result);

        const previewContainer = document.getElementById('preview-container');
        previewContainer.style.display = 'block';
        previewContainer.innerHTML = '<h5 class="mb-3">Select the images you want to download:</h5><div class="row g-3"></div>';
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
          previewContainer.innerHTML += '<p class="text-muted">No preview images found.</p>';
          document.getElementById('post-btn').disabled = false;
        }

        alert(
          'Preview download request submitted successfully!\n' +
          (result.message || 'Processing...'),
        );
      } else {
        console.error('Preview download failed:', result);
        alert(
          'Preview download failed: ' + (result.message || 'Unknown error'),
        );
      }
    } catch (err) {
      console.error(`Error: ${err}`);
      alert('Error submitting preview download request: ' + err.message);
    }

    // Clear the selection after submission
    document.getElementById('preview-btn').disabled = true;
    featureGroup.clearLayers();
  });
} else {
  console.error('Preview button not found!');
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

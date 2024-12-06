import React, { useState, useEffect } from 'react';
import { fromEvent, from, tap, catchError } from 'rxjs';
import { map, mergeMap } from 'rxjs/operators';
import JSZip from 'jszip';
import { ajax } from 'rxjs/ajax';
const ImageGrid = () => {
    const [images, setImages] = useState([]);

    const loadPhotos = () => {
        if (images.length > 3) return;
        const eventSource = new EventSource('http://localhost:8080/images/original');

        const imageStream$ = fromEvent(eventSource, 'message').pipe(
            map((event) => {
                const parsedData = JSON.parse(event.data)
                return parsedData.body;
            })
        );

        const subscription = imageStream$.subscribe({
            next: (imageData) => {
                console.log(imageData)
                setImages((prevImages) => [...prevImages,{base64:`data:image/png;base64,${imageData}`, name: "abc"}]);
            },
            error: (err) => {
                console.error('Error receiving image data:', err);
            },
            complete: () => { console.log("Complete") }
        });


        return () => {
            eventSource.close();
            subscription.unsubscribe();
        };
    };

    const uploadPhotos = (imageList) => {
        console.log("upload")
        var preparedImages=imageList.map(image=>{return{ name: image.name, base64: image.base64 }});
        processAndSendFiles(preparedImages);
        // from(imageList) 
        //   .pipe(
        //     mergeMap((image) => processAndSendFile(image), 1), 
        //     tap((updatedImage) => {
        //       setImages((prevImages) =>
        //         prevImages.map((img) =>
        //           img.key === updatedImage.key ? updatedImage : img
        //         )
        //       );
        //     }),
        //     catchError((err) => {
        //       console.error('Error uploading file:', err);
        //       return []; 
        //     })
        //   )
        //   .subscribe();
      

    };

    const processAndSendFiles = (images) => {
            const request$ = ajax({
              url: 'http://localhost:8080/images/upload',
              method: 'POST',
              headers: { 'Content-Type': 'application/json' },
              body: images
            });
    
            request$.subscribe({
              next: (res) => {
                console.log("res:",res)
              },
              error: (err) => {
                console.error(`Error uploading images`, err);
              },
            });
          
        
      };
        const processAndSendFile = (image) => {
            return new Promise((resolve, reject) => {
        
                const request$ = ajax.post({
                  url: 'http://localhost:8080/images/upload',
                  method: 'POST',
                  headers: { 'Content-Type': 'application/json' },
                  body: [{ name: image.name, base64: image.base64 }]
                });
        
                request$.subscribe({
                  next: (res) => {
                    console.log(res)
                    resolve({
                      ...image,
                      base64: res.response.base64, 
                      loaded: true, 
                    });
                  },
                  error: (err) => {
                    console.error(`Error uploading ${image.name}:`, err);
                    reject(err);
                  },
                });
              
            });
          };



    const handleFileChange = (event) => {
        const file = event.target.files[0];
        if (file && file.name.endsWith('.zip')) {
          extractZip(file);
        } else {
          alert('Please upload a valid ZIP file.');
        }
      };
      const extractZip = async (file) => {
        console.log("unzipping");
        const loadedImgs=[]
        const zip = new JSZip();
        try {
          const zipContent = await zip.loadAsync(file);
          const promises = [];
          zipContent.forEach((relativePath, zipEntry) => {
            if (isImage(zipEntry.name)) {
              const promise = zipEntry.async('base64').then((base64Data) => {
                loadedImgs.push({
                  name: zipEntry.name,
                  base64: `data:image/${getFileExtension(zipEntry.name)};base64,${base64Data}`,
                  key: generateUniqueKey(),
                  loaded: false
                });
              });
              promises.push(promise);
            }
          });
          await Promise.all(promises);
          console.log(loadedImgs)
          setImages(loadedImgs);
          uploadPhotos(loadedImgs);
        } catch (error) {
          alert('Error extracting ZIP file: ' + error.message);
        }
      };

    const isImage = (fileName) => {
        const imageExtensions = ['.jpg', '.jpeg', '.png', '.gif', '.bmp', '.webp'];
        return imageExtensions.some((ext) => fileName.toLowerCase().endsWith(ext));
      };
      const getFileExtension = (fileName) => {
        return fileName.split('.').pop().toLowerCase();
      };
    const generateUniqueKey = () => `img_${Date.now()}_${Math.random()}`;
      

    return (
        <div>
            <button onClick={loadPhotos}>Load</button>
            <input type="file" onChange={handleFileChange}/>
            {images.map((image) => (
                image.loaded ?
                <div key={image.key} style={{ margin: '10px', display: 'inline-block' }}>
                    <img
                        src={`${image.base64}`}
                        alt={`Image ${image.name}`}
                        style={{ width: '200px', height: 'auto', borderRadius: '8px' }}
                    />
                </div> 
                :  <div>
                    Loading image
                </div>
            ))}
        </div>
    );
};
export default ImageGrid;

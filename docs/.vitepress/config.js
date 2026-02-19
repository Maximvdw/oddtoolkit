import { defineConfig } from 'vitepress'

export default defineConfig({
  title: 'ODDToolkit',
  description: 'Documentation for the Ontology Driven Design Toolkit',
  themeConfig: {
    nav: [
      { text: 'Guide', link: '/guide/usage' },
    ],
    sidebar: [
      {
        text: 'Guide',
        items: [
          { text: 'Usage', link: '/guide/usage' },
        ]
      }
    ]
  }
})


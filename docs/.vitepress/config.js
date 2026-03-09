import { defineConfig } from 'vitepress'

const base = process.env.DOCS_BASE || '/'

export default defineConfig({
  title: 'ODDToolkit',
  description: 'Documentation for the Ontology Driven Design Toolkit',
  base,
  cleanUrls: true,
  themeConfig: {
    nav: [
      { text: 'Home', link: '/' },
      { text: 'Guide', link: '/guide/usage' },
      { text: 'CLI', link: '/cli-guide' },
      { text: 'Extension', link: '/extension-guide' },
      { text: 'Quickstart', link: '/QUICKSTART' }
    ],
    sidebar: [
      {
        text: 'Get Started',
        items: [
          { text: 'Overview', link: '/' },
          { text: 'Usage', link: '/guide/usage' },
          { text: 'Configuration', link: '/guide/configuration' },
          { text: 'Ontology & Metadata', link: '/guide/ontology-metadata' }
        ]
      },
      {
        text: 'Reference',
        items: [
          { text: 'CLI Guide', link: '/cli-guide' },
          { text: 'Extension Guide', link: '/extension-guide' },
          { text: 'Quickstart', link: '/QUICKSTART' }
        ]
      }
    ],
    socialLinks: [
      { icon: 'github', link: 'https://github.com/maximvdw/oddtoolkit' }
    ],
    search: {
      provider: 'local'
    }
  }
})

